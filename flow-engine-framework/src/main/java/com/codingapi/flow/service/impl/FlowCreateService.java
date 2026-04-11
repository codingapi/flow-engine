package com.codingapi.flow.service.impl;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.event.FlowRecordStartEvent;
import com.codingapi.flow.event.FlowRecordTodoEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.exception.FlowExecutionException;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.exception.FlowPermissionException;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.service.WorkflowService;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;
import com.codingapi.springboot.framework.event.EventPusher;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建流程服务
 */
public class FlowCreateService {

    private final FlowCreateRequest request;
    private final FlowOperatorGateway flowOperatorGateway;
    private final WorkflowService workflowService;
    private final IRepositoryHolder repositoryHolder;

    public FlowCreateService(FlowCreateRequest request,IRepositoryHolder repositoryHolder) {
        this.request = request;
        this.flowOperatorGateway = repositoryHolder.getFlowOperatorGateway();
        this.workflowService = repositoryHolder.getWorkflowService();
        this.repositoryHolder = repositoryHolder;
    }

    public long create() {
        request.verify();
        Workflow workflow = workflowService.getWorkflow(request.getWorkId());
        if (workflow == null) {
            throw FlowNotFoundException.workflow(request.getWorkId());
        }
        if (workflow.isDisable()) {
            throw FlowStateException.workflowAlreadyDisable(request.getWorkId());
        }
        workflow.verify();
        // 获取备份
        WorkflowRuntime workflowRuntime = workflowService.getWorkflowRuntime(workflow.getId(), workflow.getUpdatedTime());
        if (workflowRuntime == null) {
            workflowRuntime = new WorkflowRuntime(workflow);
            workflowService.saveWorkflowRuntime(workflowRuntime);
        }
        // 验证当前用户
        IFlowOperator currentOperator = flowOperatorGateway.get(request.getOperatorId());
        if (!workflow.matchCreatedOperator(currentOperator)) {
            throw FlowPermissionException.accessDenied("create workflow");
        }
        // 构建表单数据
        FormData formData = new FormData(workflow.getForm());
        formData.reset(request.getFormData());

        StartNode currentNode = (StartNode) workflow.getStartNode();
        IFlowAction action = currentNode.actionManager().getActionById(request.getActionId());
        FlowSession session = FlowSession.startSession(this.repositoryHolder,currentOperator, workflow, currentNode, action, formData, workflowRuntime.getId());

        List<FlowRecord> flowRecords = currentNode.generateCurrentRecords(session);

        // 如果存在父流程，则同步更新到该子流程上
        if (this.request.isSubProcess()) {
            flowRecords.forEach(flowRecord -> {
                flowRecord.setParentId(this.request.getParentRecordId());
            });
        }

        currentNode.verifySession(session);

        if (flowRecords.size() > 1) {
            throw FlowExecutionException.createRecordSizeError();
        }

        // 持久化 INITIATOR_SELECT 节点的操作人分配
        if (request.getOperatorSelectMap() != null
                && !request.getOperatorSelectMap().isEmpty()
                && !flowRecords.isEmpty()) {
            String processId = flowRecords.get(0).getProcessId();
            request.getOperatorSelectMap().forEach((nodeId, operatorIds) ->
                    repositoryHolder.saveOperatorAssignment(processId, nodeId, operatorIds)
            );
        }

        repositoryHolder.saveRecords(flowRecords);

        List<IFlowEvent> events = new ArrayList<>();
        for (FlowRecord flowRecord : flowRecords) {
            events.add(new FlowRecordStartEvent(flowRecord,session.isMock()));
            events.add(new FlowRecordTodoEvent(flowRecord,session.isMock()));
        }

        // 推送事件
        for (IFlowEvent event : events) {
            EventPusher.push(event);
        }

        FlowRecord currentRecord = flowRecords.get(0);
        return currentRecord.getId();
    }
}

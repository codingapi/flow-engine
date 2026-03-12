package com.codingapi.flow.service.impl;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.manager.WorkflowStrategyManager;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.service.FlowRecordService;
import com.codingapi.flow.service.WorkflowService;
import com.codingapi.flow.session.FlowAdvice;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;

/**
 * 节点动作服务
 */
public class FlowActionService {

    private final FlowActionRequest request;
    private final FlowOperatorGateway flowOperatorGateway;
    private final FlowRecordService flowRecordService;
    private final WorkflowService workflowService;

    private final IRepositoryHolder repositoryHolder;

    public FlowActionService(FlowActionRequest request,IRepositoryHolder repositoryHolder) {
        this.request = request;
        this.flowOperatorGateway = repositoryHolder.getFlowOperatorGateway();
        this.flowRecordService = repositoryHolder.getFlowRecordService();
        this.workflowService = repositoryHolder.getWorkflowService();
        this.repositoryHolder = repositoryHolder;
    }

    public void action() {

        request.verify();
        // 验证当前用户
        IFlowOperator currentOperator = flowOperatorGateway.get(request.getAdvice().getOperatorId());
        if (currentOperator == null) {
            throw FlowNotFoundException.operator(request.getAdvice().getOperatorId());
        }
        FlowRecord flowRecord = flowRecordService.getFlowRecord(request.getRecordId());
        if (flowRecord == null) {
            throw FlowNotFoundException.record(request.getRecordId());
        }
        if (!flowRecord.isTodo()) {
            throw FlowStateException.recordAlreadyDone();
        }

        WorkflowRuntime workflowRuntime = workflowService.getWorkflowRuntime(flowRecord.getWorkRuntimeId());
        if (workflowRuntime == null) {
            throw FlowNotFoundException.workflow(flowRecord.getWorkRuntimeId() + " not found");
        }

        Workflow workflow = workflowRuntime.toWorkflow();

        long recordOperatorId = flowRecord.getCurrentOperatorId();
        WorkflowStrategyManager workflowStrategyManager = workflow.strategyManager();
        workflowStrategyManager.verifyOperator(currentOperator, recordOperatorId);

        IFlowNode currentNode = workflow.getFlowNode(flowRecord.getNodeId());
        if (currentNode == null) {
            throw FlowNotFoundException.node(flowRecord.getNodeId());
        }
        IFlowAction flowAction = currentNode.actionManager().getActionById(request.getAdvice().getActionId());
        if (flowAction == null || !flowAction.enable()) {
            throw FlowNotFoundException.action(request.getAdvice().getActionId());
        }

        // 构建表单数据
        FormData formData = new FormData(workflow.getForm());
        formData.reset(request.getFormData());
        FlowAdvice flowAdvice = request.toFlowAdvice(workflow, flowAction);

        FlowSession session = flowRecord.createFlowSession(this.repositoryHolder,workflow,currentOperator,formData,flowAdvice);
        // 验证会话
        currentNode.verifySession(session);
        // 执行动作
        flowAction.run(session);

    }
}


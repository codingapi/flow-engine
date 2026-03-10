package com.codingapi.flow.service.impl;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;
import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.manager.WorkflowStrategyManager;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.repository.WorkflowRuntimeRepository;
import com.codingapi.flow.session.FlowAdvice;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.workflow.Workflow;

/**
 * 节点动作服务
 */
public class FlowActionService {

    private final FlowActionRequest request;
    private final FlowOperatorGateway flowOperatorGateway;
    private final FlowRecordRepository flowRecordRepository;
    private final WorkflowRuntimeRepository workflowRuntimeRepository;

    public FlowActionService(FlowActionRequest request) {
        this.request = request;
        this.flowOperatorGateway = RepositoryHolderContext.getInstance().getFlowOperatorGateway();
        this.flowRecordRepository = RepositoryHolderContext.getInstance().getFlowRecordRepository();
        this.workflowRuntimeRepository = RepositoryHolderContext.getInstance().getWorkflowRuntimeRepository();
    }

    public void action() {

        request.verify();
        // 验证当前用户
        IFlowOperator currentOperator = flowOperatorGateway.get(request.getAdvice().getOperatorId());
        if (currentOperator == null) {
            throw FlowNotFoundException.operator(request.getAdvice().getOperatorId());
        }
        FlowRecord flowRecord = flowRecordRepository.get(request.getRecordId());
        if (flowRecord == null) {
            throw FlowNotFoundException.record(request.getRecordId());
        }
        if (!flowRecord.isTodo()) {
            throw FlowStateException.recordAlreadyDone();
        }

        WorkflowRuntime workflowRuntime = workflowRuntimeRepository.get(flowRecord.getWorkBackupId());
        if (workflowRuntime == null) {
            throw FlowNotFoundException.workflow(flowRecord.getWorkBackupId() + " not found");
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

        FlowSession session = flowRecord.createFlowSession(workflow,currentOperator,formData,flowAdvice);
        // 验证会话
        currentNode.verifySession(session);
        // 执行动作
        flowAction.run(session);

    }
}


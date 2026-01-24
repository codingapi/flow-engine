package com.codingapi.flow.service.impl;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.backup.WorkflowBackup;
import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.repository.WorkflowBackupRepository;
import com.codingapi.flow.session.FlowAdvice;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.workflow.Workflow;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 节点动作服务
 */
@AllArgsConstructor
public class FlowActionService {

    private final FlowActionRequest request;
    private final FlowOperatorGateway flowOperatorGateway;
    private final FlowRecordRepository flowRecordRepository;
    private final WorkflowBackupRepository workflowBackupRepository;

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

        long currentOperatorId = flowRecord.getCurrentOperatorId();
        if(!currentOperator.isFlowManager()){
            if (currentOperatorId != currentOperator.getUserId()) {
                throw FlowStateException.operatorNotMatch();
            }
        }

        WorkflowBackup workflowBackup = workflowBackupRepository.get(flowRecord.getWorkBackupId());
        if (workflowBackup == null) {
            throw FlowNotFoundException.workflow(flowRecord.getWorkBackupId() + "");
        }

        Workflow workflow = workflowBackup.toWorkflow();
        IFlowNode currentNode = workflow.getFlowNode(flowRecord.getNodeId());
        if (currentNode == null) {
            throw FlowNotFoundException.node(flowRecord.getNodeId());
        }
        IFlowAction flowAction = currentNode.actionManager().getActionById(request.getAdvice().getActionId());
        if (flowAction == null) {
            throw FlowNotFoundException.action(request.getAdvice().getActionId());
        }

        // 构建表单数据
        FormData formData = new FormData(workflow.getForm());
        formData.reset(request.getFormData());
        FlowAdvice flowAdvice = request.toFlowAdvice(workflow, flowAction);

        List<FlowRecord> currentRecords = RepositoryHolderContext.getInstance().findCurrentNodeRecords(flowRecord.getFromId(), flowRecord.getNodeId());
        FlowSession session = new FlowSession(currentOperator, workflow, currentNode, flowAction, formData, flowRecord, currentRecords, workflowBackup.getId(), flowAdvice);
        // 验证会话
        currentNode.verifySession(session);
        // 执行动作
        flowAction.run(session);

    }
}


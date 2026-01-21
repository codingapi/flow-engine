package com.codingapi.flow.service.impl;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.backup.WorkflowBackup;
import com.codingapi.flow.context.RepositoryContext;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.repository.ParallelBranchRepository;
import com.codingapi.flow.repository.WorkflowBackupRepository;
import com.codingapi.flow.session.FlowAdvice;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.workflow.Workflow;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class FlowActionService {

    private final FlowActionRequest request;
    private final FlowOperatorGateway flowOperatorGateway;
    private final FlowRecordRepository flowRecordRepository;
    private final WorkflowBackupRepository workflowBackupRepository;
    private final ParallelBranchRepository parallelBranchRepository;

    public void action() {
        RepositoryContext.getInstance().setFlowRecordRepository(flowRecordRepository);
        RepositoryContext.getInstance().setFlowOperatorGateway(flowOperatorGateway);
        RepositoryContext.getInstance().setParallelBranchRepository(parallelBranchRepository);

        request.verify();
        // 验证当前用户
        IFlowOperator currentOperator = flowOperatorGateway.get(request.getAdvice().getOperatorId());
        if (currentOperator == null) {
            throw new IllegalArgumentException("currentOperator not exist");
        }
        FlowRecord flowRecord = flowRecordRepository.get(request.getRecordId());
        if (flowRecord == null) {
            throw new IllegalArgumentException("record not exist");
        }
        if (!flowRecord.isTodo()) {
            throw new IllegalArgumentException("record has done");
        }

        long currentOperatorId = flowRecord.getCurrentOperatorId();
        if (currentOperatorId != currentOperator.getUserId()) {
            throw new IllegalArgumentException("currentOperator not match");
        }

        WorkflowBackup workflowBackup = workflowBackupRepository.get(flowRecord.getWorkBackupId());
        if (workflowBackup == null) {
            throw new IllegalArgumentException("workflow not exist");
        }

        Workflow workflow = workflowBackup.toWorkflow();
        IFlowNode currentNode = workflow.getFlowNode(flowRecord.getNodeId());
        if (currentNode == null) {
            throw new IllegalArgumentException("currentNode not exist");
        }
        IFlowAction flowAction = currentNode.actionManager().getActionById(request.getAdvice().getActionId());
        if (flowAction == null) {
            throw new IllegalArgumentException("action not exist");
        }

        // 构建表单数据
        FormData formData = new FormData(workflow.getForm());
        formData.reset(request.getFormData());
        FlowAdvice flowAdvice = request.toFlowAdvice(workflow, flowAction);

        List<FlowRecord> currentRecords = RepositoryContext.getInstance().findRecordsByFromIdAndNodeId(flowRecord.getFromId(), flowRecord.getNodeId());
        FlowSession session = new FlowSession(currentOperator, workflow, currentNode, flowAction, formData, flowRecord, currentRecords, workflowBackup.getId(), flowAdvice);

        currentNode.verifySession(session);

        flowAction.run(session);

    }
}


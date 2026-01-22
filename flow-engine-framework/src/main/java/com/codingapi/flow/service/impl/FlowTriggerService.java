package com.codingapi.flow.service.impl;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.backup.WorkflowBackup;
import com.codingapi.flow.context.RepositoryContext;
import com.codingapi.flow.delay.DelayTask;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.repository.WorkflowBackupRepository;
import com.codingapi.flow.session.FlowAdvice;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.workflow.Workflow;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class FlowTriggerService {

    private final DelayTask delayTask;
    private final FlowOperatorGateway flowOperatorGateway;
    private final FlowRecordRepository flowRecordRepository;
    private final WorkflowBackupRepository workflowBackupRepository;

    /**
     * 延期任务触发执行
     */
    public void trigger() {
        FlowRecord flowRecord = flowRecordRepository.get(delayTask.getCurrentRecordId());
        if (flowRecord == null) {
            throw FlowNotFoundException.record(delayTask.getCurrentRecordId());
        }

        WorkflowBackup workflowBackup = workflowBackupRepository.get(flowRecord.getWorkBackupId());
        if (workflowBackup == null) {
            throw FlowNotFoundException.workflow(flowRecord.getWorkBackupId() + "");
        }

        Workflow workflow = workflowBackup.toWorkflow();
        IFlowNode currentNode = workflow.getFlowNode(flowRecord.getNodeId());

        IFlowOperator currentOperator = flowOperatorGateway.get(flowRecord.getCurrentOperatorId());
        IFlowAction flowAction = currentNode.actionManager().getActionById(flowRecord.getActionId());
        FormData formData = new FormData(workflow.getForm());
        formData.reset(flowRecord.getFormData());

        FlowAdvice advice = flowRecord.toAdvice(workflow);
        List<FlowRecord> currentRecords = RepositoryContext.getInstance().findRecordsByFromIdAndNodeId(flowRecord.getFromId(), flowRecord.getNodeId());

        // 获取延迟任务节点
        IFlowNode delayNode = workflow.getFlowNode(delayTask.getDelayNodeId());

        // 执行后续任务
        FlowSession flowSession = new FlowSession(currentOperator, workflow, delayNode, flowAction, formData, flowRecord, currentRecords, flowRecord.getWorkBackupId(), advice);
        flowAction.run(flowSession);

    }
}


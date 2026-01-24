package com.codingapi.flow.service.impl;

import com.codingapi.flow.backup.WorkflowBackup;
import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.event.FlowRecordTodoEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.manager.StrategyManager;
import com.codingapi.flow.pojo.request.FlowRevokeRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.repository.WorkflowBackupRepository;
import com.codingapi.flow.strategy.node.RevokeStrategy;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.springboot.framework.event.EventPusher;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 撤销流程服务
 */
@AllArgsConstructor
public class FlowRevokeService {

    private final FlowRevokeRequest request;
    private final FlowRecordRepository flowRecordRepository;
    private final WorkflowBackupRepository workflowBackupRepository;

    public void revoke() {
        request.verify();
        // 验证当前用户
        FlowRecord currentRecord = flowRecordRepository.get(request.getRecordId());
        if (currentRecord == null) {
            throw FlowNotFoundException.record(request.getRecordId());
        }
        if (currentRecord.isTodo()) {
            throw FlowStateException.recordAlreadyTodo();
        }
        if (currentRecord.isFinish()) {
            throw FlowStateException.recordNotSupportRevoke();
        }
        long currentOperatorId = currentRecord.getCurrentOperatorId();
        if (currentOperatorId != request.getOperatorId()) {
            throw FlowStateException.operatorNotMatch();
        }
        WorkflowBackup workflowBackup = workflowBackupRepository.get(currentRecord.getWorkBackupId());
        if (workflowBackup == null) {
            throw FlowNotFoundException.workflow(currentRecord.getWorkBackupId() + " not found");
        }
        Workflow workflow = workflowBackup.toWorkflow();
        IFlowNode currentNode = workflow.getFlowNode(currentRecord.getNodeId());
        StrategyManager strategyManager = currentNode.strategyManager();
        RevokeStrategy revokeStrategy = strategyManager.getStrategy(RevokeStrategy.class);
        if (revokeStrategy == null || !revokeStrategy.isEnable()) {
            throw FlowStateException.nodeNotSupportRevoke();
        }

        List<FlowRecord> afterRecords = flowRecordRepository.findAfterRecords(currentRecord.getProcessId(), currentRecord.getId());
        // 退回下级记录, 如果下级记录都完成则不允许退回
        if (revokeStrategy.isRemoveNext()) {
            List<FlowRecord> nextRecords = afterRecords.stream()
                    .filter(flowRecord -> flowRecord.getFromId() == currentRecord.getId())
                    .toList();
            boolean nextRecordDone = true;
            for (FlowRecord nextRecord : nextRecords) {
                if (nextRecord.isTodo()) {
                    nextRecordDone = false;
                }
            }
            if (nextRecordDone) {
                throw FlowStateException.recordNotSupportRevoke();
            }
        }

        List<FlowRecord> recordList = new ArrayList<>();
        List<IFlowEvent> flowEvents = new ArrayList<>();

        currentRecord.clearDone();

        recordList.add(currentRecord);
        flowEvents.add(new FlowRecordTodoEvent(currentRecord));

        for (FlowRecord afterRecord : afterRecords) {
            afterRecord.revoke();
            recordList.add(afterRecord);
        }

        RepositoryHolderContext.getInstance().saveRecords(recordList);
        flowEvents.forEach(EventPusher::push);

    }
}


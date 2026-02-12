package com.codingapi.flow.service.impl;

import com.codingapi.flow.backup.WorkflowBackup;
import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.domain.UrgeInterval;
import com.codingapi.flow.event.FlowRecordUrgeEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.manager.WorkflowStrategyManager;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.FlowUrgeRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.repository.UrgeIntervalRepository;
import com.codingapi.flow.repository.WorkflowBackupRepository;
import com.codingapi.flow.strategy.workflow.UrgeStrategy;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.springboot.framework.event.EventPusher;

import java.util.ArrayList;
import java.util.List;

/**
 * 催办服务
 */
public class FlowUrgeService {

    private final FlowUrgeRequest request;
    private final FlowRecordRepository flowRecordRepository;
    private final FlowOperatorGateway flowOperatorGateway;
    private final UrgeIntervalRepository urgeIntervalRepository;
    private final WorkflowBackupRepository workflowBackupRepository;

    public FlowUrgeService(FlowUrgeRequest request) {
        this.request = request;
        this.flowRecordRepository = RepositoryHolderContext.getInstance().getFlowRecordRepository();
        this.flowOperatorGateway = RepositoryHolderContext.getInstance().getFlowOperatorGateway();
        this.urgeIntervalRepository = RepositoryHolderContext.getInstance().getUrgeIntervalRepository();
        this.workflowBackupRepository = RepositoryHolderContext.getInstance().getWorkflowBackupRepository();
    }

    /**
     * 催办
     */
    public void urge() {
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

        UrgeInterval urgeInterval = urgeIntervalRepository.getLatest(currentRecord.getProcessId(), request.getRecordId());
        if (urgeInterval != null) {
            WorkflowBackup workflowBackup = workflowBackupRepository.get(currentRecord.getWorkBackupId());
            Workflow workflow = workflowBackup.toWorkflow();
            WorkflowStrategyManager strategyManager = workflow.strategyManager();
            if (strategyManager.isEnableUrge()) {
                UrgeStrategy urgeStrategy = strategyManager.getStrategy(UrgeStrategy.class);
                if (!urgeStrategy.hasUrge(urgeInterval)) {
                    throw FlowStateException.recordNotSupportUrge();
                }
            }
        }

        IFlowOperator currentOperator = flowOperatorGateway.get(currentOperatorId);

        List<FlowRecord> todoRecords = flowRecordRepository.findTodoRecords(currentRecord.getProcessId());
        // 保存催办记录
        urgeIntervalRepository.save(new UrgeInterval(currentRecord));

        List<IFlowEvent> flowEvents = new ArrayList<>();

        for (FlowRecord todoRecord : todoRecords) {
            flowEvents.add(new FlowRecordUrgeEvent(todoRecord, currentOperator));
        }

        flowEvents.forEach(EventPusher::push);
    }
}

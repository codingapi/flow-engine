package com.codingapi.flow.service.impl;

import com.codingapi.flow.backup.WorkflowBackup;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.response.FlowContent;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.repository.WorkflowBackupRepository;
import com.codingapi.flow.repository.WorkflowRepository;
import com.codingapi.flow.workflow.Workflow;
import lombok.AllArgsConstructor;

public class FlowDetailService {

    private final String id;
    private final IFlowOperator currentOperator;
    private final FlowRecordRepository flowRecordRepository;
    private final FlowOperatorGateway flowOperatorGateway;
    private final WorkflowRepository workflowRepository;
    private final WorkflowBackupRepository workflowBackupRepository;

    public FlowDetailService(String id,
                             IFlowOperator currentOperator,
                             FlowRecordRepository flowRecordRepository,
                             FlowOperatorGateway flowOperatorGateway,
                             WorkflowRepository workflowRepository,
                             WorkflowBackupRepository workflowBackupRepository) {
        this.id = id;
        this.currentOperator = currentOperator;
        this.flowRecordRepository = flowRecordRepository;
        this.flowOperatorGateway = flowOperatorGateway;
        this.workflowRepository = workflowRepository;
        this.workflowBackupRepository = workflowBackupRepository;
    }


    private boolean isCreateWorkflow() {
        return !id.matches("^[0-9]+$");
    }

    public FlowContent detail() {
        if (this.isCreateWorkflow()) {
            Workflow workflow = workflowRepository.get(id);
            if (workflow == null) {
                throw FlowNotFoundException.workflow(id);
            }
            return new FlowContentFactory(workflow, null).create();
        } else {
            FlowRecord flowRecord = flowRecordRepository.get(Long.parseLong(id));
            if (flowRecord == null) {
                throw FlowNotFoundException.record(Long.parseLong(id));
            }
            WorkflowBackup workflowBackup = workflowBackupRepository.get(flowRecord.getWorkBackupId());
            if (workflowBackup == null) {
                throw FlowNotFoundException.workflow(flowRecord.getWorkBackupId() + " not found");
            }
            Workflow workflow = workflowBackup.toWorkflow();
            return new FlowContentFactory(workflow, flowRecord).create();
        }
    }


    @AllArgsConstructor
    private static class FlowContentFactory {
        private final Workflow workflow;
        private final FlowRecord flowRecord;

        public FlowContent create() {
            return new FlowContent();
        }
    }
}

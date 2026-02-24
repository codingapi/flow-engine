package com.codingapi.flow.service.impl;

import com.codingapi.flow.backup.WorkflowBackup;
import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.FlowDetailRequest;
import com.codingapi.flow.pojo.response.FlowContent;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.record.FlowTodoMerge;
import com.codingapi.flow.record.FlowTodoRecord;
import com.codingapi.flow.repository.*;
import com.codingapi.flow.workflow.Workflow;

import java.util.List;

/**
 *  流程详情服务
 */
public class FlowDetailService {

    private final FlowDetailRequest request;
    private final IFlowOperator currentOperator;
    private final FlowRecordRepository flowRecordRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowBackupRepository workflowBackupRepository;

    public FlowDetailService(FlowDetailRequest request) {
        this.request = request;
        this.currentOperator = RepositoryHolderContext.getInstance().getOperatorById(request.getOperatorId());
        this.flowRecordRepository = RepositoryHolderContext.getInstance().getFlowRecordRepository();
        this.workflowRepository = RepositoryHolderContext.getInstance().getWorkflowRepository();
        this.workflowBackupRepository = RepositoryHolderContext.getInstance().getWorkflowBackupRepository();
    }

    public FlowContent detail() {
        if (this.request.isCreateWorkflow()) {
            Workflow workflow = workflowRepository.get(this.request.getId());
            if (workflow == null) {
                throw FlowNotFoundException.workflow(this.request.getId());
            }
            return new FlowContentFactory(workflow, null, currentOperator).create();
        } else {
            FlowRecord flowRecord = flowRecordRepository.get(Long.parseLong(this.request.getId()));
            if (flowRecord == null) {
                throw FlowNotFoundException.record(Long.parseLong(this.request.getId()));
            }
            WorkflowBackup workflowBackup = workflowBackupRepository.get(flowRecord.getWorkBackupId());
            if (workflowBackup == null) {
                throw FlowNotFoundException.workflow(flowRecord.getWorkBackupId() + " not found");
            }
            Workflow workflow = workflowBackup.toWorkflow();

            if(!flowRecord.isReadable()){
                flowRecord.read();
                RepositoryHolderContext.getInstance().saveRecord(flowRecord);
            }

            return new FlowContentFactory(workflow, flowRecord,currentOperator).create();
        }
    }


    private static class FlowContentFactory {
        private final IFlowOperator currentOperator;
        private final Workflow workflow;
        private final FlowRecord flowRecord;
        private final FlowContent flowContent;

        private final FlowTodoMergeRepository flowTodoMergeRepository;
        private final FlowTodoRecordRepository flowTodoRecordRepository;
        private final FlowRecordRepository flowRecordRepository;

        public FlowContentFactory(Workflow workflow, FlowRecord flowRecord,IFlowOperator currentOperator) {
            this.workflow = workflow;
            this.flowRecord = flowRecord;
            this.currentOperator = currentOperator;
            this.flowTodoMergeRepository = RepositoryHolderContext.getInstance().getFlowTodoMergeRepository();
            this.flowTodoRecordRepository = RepositoryHolderContext.getInstance().getFlowTodoRecordRepository();
            this.flowRecordRepository = RepositoryHolderContext.getInstance().getFlowRecordRepository();
            this.flowContent = new FlowContent();

        }

        private void loadCurrentOperator(){
            this.flowContent.pushCurrentOperator(currentOperator);
        }

        private void loadWorkflow(){
            this.flowContent.pushWorkflow(workflow);
        }

        private void loadTodoFlowRecords(){
            if(this.flowRecord!=null){
                if(this.flowRecord.isMergeable()){
                    FlowTodoRecord todoRecord = flowTodoRecordRepository.getByMergeKey(flowRecord.getMergeKey());
                    List<FlowTodoMerge> todoMerges = flowTodoMergeRepository.findByTodoId(todoRecord.getId());
                    List<FlowRecord> margeRecords = flowRecordRepository.findByIds(todoMerges.stream().map(FlowTodoMerge::getRecordId).toList());
                    this.flowContent.pushRecords(this.flowRecord, margeRecords);
                }else {
                    this.flowContent.pushRecords(this.flowRecord,List.of(this.flowRecord));
                }
            }
        }


        private void loadHistoryRecords() {
            if (flowRecord != null) {
                List<FlowRecord> historyRecords = flowRecordRepository.findBeforeRecords(flowRecord.getProcessId(), flowRecord.getId());
                this.flowContent.pushHistory(workflow,historyRecords);
            }
        }

        private void loadCurrentNode(){
            IFlowNode currentNode = null;
            if(flowRecord!=null){
                currentNode = workflow.getFlowNode(flowRecord.getNodeId());
            }else {
                currentNode = workflow.getStartNode();
            }
            if(currentNode!=null) {
                this.flowContent.pushCurrentNode(currentNode);
            }
        }


        public FlowContent create() {
            this.loadCurrentOperator();
            this.loadWorkflow();
            this.loadCurrentNode();
            this.loadTodoFlowRecords();
            this.loadHistoryRecords();
            return flowContent;
        }
    }
}

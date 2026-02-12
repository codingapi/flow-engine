package com.codingapi.flow.service.impl;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.PassAction;
import com.codingapi.flow.backup.WorkflowBackup;
import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.manager.ActionManager;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.response.FlowContent;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.record.FlowTodoMerge;
import com.codingapi.flow.record.FlowTodoRecord;
import com.codingapi.flow.repository.*;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.workflow.Workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FlowDetailService {

    private final String id;
    private final IFlowOperator currentOperator;
    private final FlowRecordRepository flowRecordRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowBackupRepository workflowBackupRepository;

    public FlowDetailService(String id, IFlowOperator currentOperator) {
        this.id = id;
        this.currentOperator = currentOperator;
        this.flowRecordRepository = RepositoryHolderContext.getInstance().getFlowRecordRepository();
        this.workflowRepository = RepositoryHolderContext.getInstance().getWorkflowRepository();
        this.workflowBackupRepository = RepositoryHolderContext.getInstance().getWorkflowBackupRepository();
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
            return new FlowContentFactory(workflow, null, currentOperator).create();
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

        private void loadNextNodes() {
            List<IFlowNode> nextNodes = new ArrayList<>();
            IFlowNode currentNode = null;
            FlowSession flowSession = null;
            if (flowRecord != null) {
                currentNode = workflow.getFlowNode(flowRecord.getNodeId());
                FormData formData = new FormData(workflow.getForm());
                formData.reset(flowRecord.getFormData());
                flowSession = flowRecord.createFlowSession(workflow,currentOperator,formData,flowRecord.toAdvice(workflow));
            }else {
                currentNode = workflow.getStartNode();
                ActionManager actionManager = currentNode.actionManager();
                IFlowAction flowAction = actionManager.getAction(PassAction.class);
                flowSession = FlowSession.startSession(currentOperator, workflow, currentNode, flowAction, null, 0);
            }
            if (currentNode != null) {
                List<IFlowNode> nodes = workflow.nextNodes(currentNode);
                if(nodes!=null && !nodes.isEmpty()){
                    nextNodes.addAll(nodes);
                }
            }

            this.flowContent.pushNextNodes(flowSession,nextNodes);
            if(currentNode!=null) {
                this.flowContent.pushCurrentNode(currentNode);
            }
        }


        public FlowContent create() {
            this.loadCurrentOperator();
            this.loadWorkflow();
            this.loadNextNodes();
            this.loadTodoFlowRecords();
            this.loadHistoryRecords();

            return flowContent;
        }
    }
}

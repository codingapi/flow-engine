package com.codingapi.flow.service.impl;

import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.FlowDetailRequest;
import com.codingapi.flow.pojo.response.FlowContent;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.service.FlowRecordService;
import com.codingapi.flow.service.WorkflowService;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;

import java.util.List;

/**
 *  流程详情服务
 */
public class FlowDetailService {

    private final FlowDetailRequest request;
    private final IFlowOperator currentOperator;
    private final FlowRecordService flowRecordService;
    private final WorkflowService workflowService;
    private final IRepositoryHolder repositoryHolder;

    public FlowDetailService(FlowDetailRequest request, IRepositoryHolder repositoryHolder) {
        this.request = request;
        this.currentOperator = repositoryHolder.getOperatorById(request.getOperatorId());
        this.flowRecordService = repositoryHolder.getFlowRecordService();
        this.workflowService = repositoryHolder.getWorkflowService();
        this.repositoryHolder = repositoryHolder;
    }

    public FlowContent detail() {
        if (this.request.isCreateWorkflow()) {
            Workflow workflow = workflowService.getWorkflow(this.request.getId());
            if (workflow == null) {
                throw FlowNotFoundException.workflow(this.request.getId());
            }
            return new FlowContentFactory(workflow, null, currentOperator,repositoryHolder.getFlowRecordService()).create();
        } else {
            FlowRecord flowRecord = flowRecordService.getFlowRecord(Long.parseLong(this.request.getId()));
            if (flowRecord == null) {
                throw FlowNotFoundException.record(Long.parseLong(this.request.getId()));
            }
            WorkflowRuntime workflowRuntime = workflowService.getWorkflowRuntime(flowRecord.getWorkRuntimeId());
            if (workflowRuntime == null) {
                throw FlowNotFoundException.workflow(flowRecord.getWorkRuntimeId() + " not found");
            }
            Workflow workflow = workflowRuntime.toWorkflow();

            if(!flowRecord.isReadable()){
                flowRecord.read();
                repositoryHolder.saveRecord(flowRecord);
            }

            return new FlowContentFactory(workflow, flowRecord,currentOperator,repositoryHolder.getFlowRecordService()).create();
        }
    }


    private static class FlowContentFactory {
        private final IFlowOperator currentOperator;
        private final Workflow workflow;
        private final FlowRecord flowRecord;
        private final FlowContent flowContent;

        private final FlowRecordService flowRecordService;

        public FlowContentFactory(Workflow workflow, FlowRecord flowRecord,IFlowOperator currentOperator,FlowRecordService flowRecordService) {
            this.workflow = workflow;
            this.flowRecord = flowRecord;
            this.currentOperator = currentOperator;
            this.flowRecordService = flowRecordService;
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
                if(this.flowRecord.isMergeable() && this.flowRecord.isTodo()){
                    List<FlowRecord> margeRecords = flowRecordService.getMergeRecord(flowRecord.getTodoKey());
                    this.flowContent.pushRecords(this.flowRecord, margeRecords);
                }else {
                    this.flowContent.pushRecords(this.flowRecord,List.of(this.flowRecord));
                }
            }
        }


        private void loadHistoryRecords() {
            if (flowRecord != null) {
                List<FlowRecord> historyRecords = flowRecordService.findFlowRecordBeforeRecords(flowRecord.getProcessId(), flowRecord.getId());
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

            flowContent.setOperationAction(this.workflow,this.flowRecord);
            return flowContent;
        }
    }
}

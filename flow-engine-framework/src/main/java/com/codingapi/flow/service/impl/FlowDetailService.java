package com.codingapi.flow.service.impl;

import com.codingapi.flow.cache.FlowRuntimeScriptLocalCache;

import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.helper.BackNodeHelper;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.FlowDetailRequest;
import com.codingapi.flow.pojo.response.FlowContent;
import com.codingapi.flow.pojo.response.NodeOption;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.service.FlowRecordService;
import com.codingapi.flow.service.WorkflowService;
import com.codingapi.flow.session.FlowAdvice;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;

import groovyjarjarantlr4.v4.parse.ANTLRParser.throwsSpec_return;

import java.util.List;

/**
 * 流程详情服务
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
            Workflow workflow = workflowService.getWorkflowByCode(this.request.getId());
            if (workflow == null) {
                throw FlowNotFoundException.workflow(this.request.getId());
            }

            IFlowNode flowNode = workflow.getStartNode();

            FlowSession flowSession = FlowSession.startSession(repositoryHolder, currentOperator, workflow, flowNode,
                    null, null, 0);

            return new FlowContentFactory(flowSession, workflow, null, currentOperator,
                    repositoryHolder.getFlowRecordService()).create();
        } else {
            FlowRecord flowRecord = flowRecordService.getFlowRecord(Long.parseLong(this.request.getId()));
            if (flowRecord == null) {
                throw FlowNotFoundException.record(Long.parseLong(this.request.getId()));
            }
            WorkflowRuntime workflowRuntime = workflowService.getWorkflowRuntime(flowRecord.getWorkRuntimeId());
            if (workflowRuntime == null) {
                throw FlowNotFoundException.workflow(flowRecord.getWorkRuntimeId() + " not found");
            }
            FlowRuntimeScriptLocalCache.getInstance().set(workflowRuntime.getScripts());
            Workflow workflow = workflowRuntime.toWorkflow();

            if (!flowRecord.isReadable()) {
                flowRecord.read();
                repositoryHolder.saveRecord(flowRecord);
            }

            IFlowOperator createOperator = repositoryHolder.getOperatorById(flowRecord.getCreateOperatorId());

            IFlowOperator submitOperator = repositoryHolder.getOperatorById(flowRecord.getSubmitOperatorId());

            FlowForm flowForm = workflow.getForm();
            FormData formData = new FormData(flowForm);
            formData.reset(flowRecord.getFormData());

            FlowAdvice flowAdvice = new FlowAdvice(flowRecord.getAdvice());

            FlowSession flowSession = flowRecord.createFlowSession(repositoryHolder, workflow, currentOperator,
                    createOperator, submitOperator, formData, flowAdvice);

            FlowContent flowContent = new FlowContentFactory(flowSession, workflow, flowRecord, currentOperator,
                    repositoryHolder.getFlowRecordService()).create();
            // 子流程重置为独立能力（非审批动作）：仅当当前待办位于已开启重置能力、
            // 且已汇聚完成的子流程下游时携带标识，由前端自行决定交互呈现
            flowContent.setResetSubProcess(FlowSubProcessResetService.canReset(flowRecord, repositoryHolder));
            // 二次发起为独立能力（非审批动作）：仅当记录已正常完成且当前操作人为流程管理员时携带标识
            flowContent.setReissue(FlowReissueService.canReissue(flowRecord, currentOperator));
            return flowContent;
        }
    }

    private static class FlowContentFactory {
        private final IFlowOperator currentOperator;
        private final Workflow workflow;
        private final FlowRecord flowRecord;
        private final FlowContent flowContent;
        private final FlowSession flowSession;

        private final FlowRecordService flowRecordService;

        public FlowContentFactory(FlowSession flowSession, Workflow workflow, FlowRecord flowRecord,
                IFlowOperator currentOperator, FlowRecordService flowRecordService) {
            this.flowSession = flowSession;
            this.workflow = workflow;
            this.flowRecord = flowRecord;
            this.currentOperator = currentOperator;
            this.flowRecordService = flowRecordService;
            this.flowContent = new FlowContent();

        }

        private void loadCurrentOperator() {
            this.flowContent.pushCurrentOperator(currentOperator);
        }

        private void loadWorkflow() {
            this.flowContent.pushWorkflow(workflow);
        }

        private void loadTodoFlowRecords() {
            if (this.flowRecord != null) {
                if (this.flowRecord.isMergeable() && this.flowRecord.isTodo()) {
                    List<FlowRecord> margeRecords = flowRecordService.getMergeRecord(flowRecord.getTodoKey());
                    this.flowContent.pushRecords(this.flowRecord, margeRecords);
                } else {
                    this.flowContent.pushRecords(this.flowRecord, List.of(this.flowRecord));
                }
            }
        }

        private void loadHistoryRecords() {
            if (flowRecord != null) {
                List<FlowRecord> historyRecords = flowRecordService
                        .findFlowRecordBeforeRecords(flowRecord.getProcessId(), flowRecord.getId());
                this.flowContent.pushHistory(workflow, historyRecords);
            }
        }

        private void loadCurrentNode() {
            this.flowContent.pushCurrentNode(this.flowSession);
        }

        /**
         * 加载可退回节点。
         *
         * <p>候选来自当前记录真实执行过的历史记录（而不是流程定义的全量节点），
         * 由 {@link BackNodeHelper#historyBackNodes} 按最近优先输出，供前端直接渲染退回候选列表。
         * 必须在 {@link #loadCurrentNode()} 之前执行：没有可退回节点时，退回按钮不会下发到展示按钮集合。</p>
         */
        private void loadBackNodes() {
            if (this.flowRecord == null) {
                this.flowContent.setBackNodes(List.of());
                return;
            }
            List<FlowRecord> beforeRecords = flowRecordService.findFlowRecordBeforeRecords(
                    this.flowRecord.getProcessId(), this.flowRecord.getId());
            List<IFlowNode> backNodes = BackNodeHelper.historyBackNodes(
                    this.workflow, beforeRecords, this.flowRecord.getNodeId());
            this.flowContent.setBackNodes(backNodes.stream().map(NodeOption::new).toList());
        }

        public FlowContent create() {
            this.loadCurrentOperator();
            this.loadWorkflow();
            this.loadBackNodes();
            this.loadCurrentNode();
            this.loadTodoFlowRecords();
            this.loadHistoryRecords();

            flowContent.setOperationAction(this.workflow, this.flowRecord);
            return flowContent;
        }
    }
}

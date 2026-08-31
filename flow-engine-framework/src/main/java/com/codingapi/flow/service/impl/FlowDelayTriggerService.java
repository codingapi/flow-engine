package com.codingapi.flow.service.impl;

import com.codingapi.flow.cache.FlowRuntimeScriptLocalCache;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.PassAction;
import com.codingapi.flow.cache.FlowOperatorLocalThreadCache;
import com.codingapi.flow.domain.DelayTask;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.service.FlowRecordService;
import com.codingapi.flow.service.WorkflowService;
import com.codingapi.flow.session.FlowAdvice;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;

import java.util.List;

/**
 * 延时触发服务
 */
public class FlowDelayTriggerService {

    private final DelayTask delayTask;
    private final FlowOperatorGateway flowOperatorGateway;
    private final FlowRecordService flowRecordService;
    private final WorkflowService workflowService;

    private final IRepositoryHolder repositoryHolder;

    public FlowDelayTriggerService(DelayTask delayTask, IRepositoryHolder repositoryHolder) {
        this.delayTask = delayTask;
        this.flowOperatorGateway = repositoryHolder.getFlowOperatorGateway();
        this.flowRecordService = repositoryHolder.getFlowRecordService();
        this.workflowService = repositoryHolder.getWorkflowService();
        this.repositoryHolder = repositoryHolder;
    }

    /**
     * 延期任务触发执行
     */
    public void trigger() {
        FlowRecord flowRecord = flowRecordService.getFlowRecord(delayTask.getCurrentRecordId());
        if (flowRecord == null) {
            throw FlowNotFoundException.record(delayTask.getCurrentRecordId());
        }

        WorkflowRuntime workflowRuntime = workflowService.getWorkflowRuntime(flowRecord.getWorkRuntimeId());
        if (workflowRuntime == null) {
            throw FlowNotFoundException.workflow(flowRecord.getWorkRuntimeId() + " not found");
        }

        FlowRuntimeScriptLocalCache.getInstance().set(workflowRuntime.getScripts());
        Workflow workflow = workflowRuntime.toWorkflow();
        IFlowNode currentNode = workflow.getFlowNode(flowRecord.getNodeId());

        IFlowOperator createdOperator = flowOperatorGateway.get(flowRecord.getCreateOperatorId());
        IFlowOperator submitOperator = flowOperatorGateway.get(flowRecord.getSubmitOperatorId());
        IFlowOperator currentOperator = flowOperatorGateway.get(flowRecord.getCurrentOperatorId());
        IFlowAction flowAction = currentNode.actionManager().getActionById(flowRecord.getActionId());
        // 延迟任务固化的前驱记录可能为相同人员自动通过的留痕记录（无审批动作，actionId=null，issue #226）：
        // 以该记录所在审批节点的通过动作兜底恢复流转，避免 NPE 使流程永久停滞在延迟节点
        if (flowAction == null && flowRecord.isAutoDone()) {
            flowAction = currentNode.actionManager().getAction(PassAction.class);
        }
        FormData formData = new FormData(workflow.getForm());
        formData.reset(flowRecord.getFormData());

        FlowAdvice advice = flowRecord.toAdvice(workflow);
        List<FlowRecord> currentRecords = repositoryHolder.findCurrentNodeRecords(flowRecord.getFromId(), flowRecord.getNodeId());

        // 获取延迟任务节点
        IFlowNode delayNode = workflow.getFlowNode(delayTask.getDelayNodeId());

        // 执行后续任务
        FlowSession flowSession = new FlowSession(this.repositoryHolder,currentOperator,createdOperator,submitOperator, workflow, delayNode, flowAction, formData, flowRecord, currentRecords, flowRecord.getWorkRuntimeId(), advice);
        flowAction.run(flowSession);

    }
}


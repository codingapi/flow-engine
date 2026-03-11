package com.codingapi.flow.service.impl;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.context.RepositoryHolderContext;
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
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 延时触发服务
 */
@AllArgsConstructor
public class FlowDelayTriggerService {

    private final DelayTask delayTask;
    private final FlowOperatorGateway flowOperatorGateway;
    private final FlowRecordService flowRecordService;
    private final WorkflowService workflowService;

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

        Workflow workflow = workflowRuntime.toWorkflow();
        IFlowNode currentNode = workflow.getFlowNode(flowRecord.getNodeId());

        IFlowOperator currentOperator = flowOperatorGateway.get(flowRecord.getCurrentOperatorId());
        IFlowAction flowAction = currentNode.actionManager().getActionById(flowRecord.getActionId());
        FormData formData = new FormData(workflow.getForm());
        formData.reset(flowRecord.getFormData());

        FlowAdvice advice = flowRecord.toAdvice(workflow);
        List<FlowRecord> currentRecords = RepositoryHolderContext.getInstance().findCurrentNodeRecords(flowRecord.getFromId(), flowRecord.getNodeId());

        // 获取延迟任务节点
        IFlowNode delayNode = workflow.getFlowNode(delayTask.getDelayNodeId());

        // 执行后续任务
        FlowSession flowSession = new FlowSession(currentOperator, workflow, delayNode, flowAction, formData, flowRecord, currentRecords, flowRecord.getWorkRuntimeId(), advice);
        flowAction.run(flowSession);

    }
}


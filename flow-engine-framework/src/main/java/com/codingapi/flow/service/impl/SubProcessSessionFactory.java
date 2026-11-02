package com.codingapi.flow.service.impl;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.PassAction;
import com.codingapi.flow.cache.FlowRuntimeScriptLocalCache;
import com.codingapi.flow.domain.SubProcessRecord;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.service.WorkflowService;
import com.codingapi.flow.session.FlowAdvice;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;

import java.util.List;

/**
 * 子流程父流程会话工厂。
 *
 * <p>以子流程聚合记录的锚点记录为来源重建父流程会话，供子流程结果判定脚本与
 * 子流程重置重建脚本复用，保证两处脚本执行时的父流程上下文、表单数据与动作信息一致。</p>
 */
public class SubProcessSessionFactory {

    private SubProcessSessionFactory() {
    }

    /**
     * 以子流程聚合记录的锚点记录重建父流程会话。
     *
     * <p>当前记录为锚点记录、当前节点为子流程节点，表单数据与审批意见均来自锚点记录，
     * 因而与首次触发子流程时的脚本上下文保持一致。</p>
     *
     * @param subProcessRecord 子流程聚合记录
     * @param repositoryHolder 仓储持有者
     * @param workflowService  流程服务
     * @return 父流程会话
     */
    public static FlowSession createParentSession(SubProcessRecord subProcessRecord,
                                                  IRepositoryHolder repositoryHolder,
                                                  WorkflowService workflowService) {
        FlowRecord parentRecord = repositoryHolder.getRecordById(subProcessRecord.getParentRecordId());
        if (parentRecord == null) {
            throw FlowNotFoundException.record(subProcessRecord.getParentRecordId());
        }
        WorkflowRuntime workflowRuntime = workflowService.getWorkflowRuntime(subProcessRecord.getParentWorkRuntimeId());
        if (workflowRuntime == null) {
            throw FlowNotFoundException.workflow(subProcessRecord.getParentWorkRuntimeId() + " not found");
        }
        FlowRuntimeScriptLocalCache.getInstance().set(workflowRuntime.getScripts());
        Workflow workflow = workflowRuntime.toWorkflow();
        IFlowNode sourceNode = workflow.getFlowNode(parentRecord.getNodeId());
        IFlowAction action = sourceNode.actionManager().getActionById(parentRecord.getActionId());
        // 前驱记录可能为相同人员自动通过的留痕记录（无审批动作，actionId=null，issue #226）：
        // 恢复下游以来源节点的通过动作兜底，避免空动作会话导致 NPE 使主流程永久停滞
        if (action == null && parentRecord.isAutoDone()) {
            action = sourceNode.actionManager().getAction(PassAction.class);
        }
        IFlowOperator currentOperator = repositoryHolder.getOperatorById(parentRecord.getCurrentOperatorId());
        IFlowOperator createdOperator = repositoryHolder.getOperatorById(parentRecord.getCreateOperatorId());
        IFlowOperator submitOperator = repositoryHolder.getOperatorById(parentRecord.getSubmitOperatorId());
        FormData formData = new FormData(workflow.getForm());
        formData.reset(parentRecord.getFormData());
        FlowAdvice advice = parentRecord.toAdvice(workflow);
        IFlowNode subProcessNode = workflow.getFlowNode(subProcessRecord.getNodeId());
        return new FlowSession(repositoryHolder,
                currentOperator,
                createdOperator,
                submitOperator,
                workflow,
                subProcessNode,
                action,
                formData,
                parentRecord,
                List.of(),
                parentRecord.getWorkRuntimeId(),
                advice);
    }
}
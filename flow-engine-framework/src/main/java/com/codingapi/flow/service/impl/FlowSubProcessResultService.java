package com.codingapi.flow.service.impl;

import com.codingapi.flow.action.BaseAction;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.PassAction;
import com.codingapi.flow.cache.FlowRuntimeScriptLocalCache;
import com.codingapi.flow.domain.SubProcessContext;
import com.codingapi.flow.domain.SubProcessRecord;
import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.event.FlowRecordDoneEvent;
import com.codingapi.flow.event.FlowRecordTodoEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.SubProcessNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.SubProcessRepository;
import com.codingapi.flow.service.WorkflowService;
import com.codingapi.flow.session.FlowAdvice;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.strategy.node.SubProcessStrategy;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;
import com.codingapi.springboot.framework.event.EventPusher;

import java.util.ArrayList;
import java.util.List;

/**
 * 子流程结束后的结果判定与主流程恢复服务。
 */
public class FlowSubProcessResultService {

    private final FlowRecord childFinalRecord;
    private final IRepositoryHolder repositoryHolder;
    private final SubProcessRepository subProcessRepository;
    private final WorkflowService workflowService;

    public FlowSubProcessResultService(FlowRecord childFinalRecord, IRepositoryHolder repositoryHolder) {
        this.childFinalRecord = childFinalRecord;
        this.repositoryHolder = repositoryHolder;
        this.subProcessRepository = repositoryHolder.getSubProcessRepository();
        this.workflowService = repositoryHolder.getWorkflowService();
    }

    public void complete() {
        if (childFinalRecord.getParentId() <= 0) {
            return;
        }
        SubProcessRecord subProcessRecord = subProcessRepository
                .findByParentRecordId(childFinalRecord.getParentId()).stream()
                // 已取代的聚合组不参与结果判定，避免继承实例同时归属新旧两组时命中旧组导致主流程停滞
                .filter(record -> !record.isSuperseded())
                .filter(record -> record.containsChildProcess(childFinalRecord.getProcessId()))
                .findFirst()
                .orElse(null);
        if (subProcessRecord == null || !subProcessRecord.complete(childFinalRecord)) {
            return;
        }
        subProcessRepository.save(subProcessRecord);
        if (!subProcessRecord.isWaiting()) {
            // 结果脚本可能提前放行主流程，剩余子流程仍要更新实例最终状态，
            // 但聚合状态已终结时不能再次执行结果脚本或恢复主流程。
            return;
        }

        FlowSession session = createParentSession(subProcessRecord);
        SubProcessNode subProcessNode = (SubProcessNode) session.getCurrentNode();
        SubProcessStrategy strategy = subProcessNode.strategyManager().getStrategy(SubProcessStrategy.class);
        if (strategy.confirm(session)) {
            subProcessRecord.pass();
            subProcessRepository.save(subProcessRecord);
            resumeNextNodes(session);
            return;
        }
        if (subProcessRecord.isAllFinished()) {
            ErrorThrow errorThrow = subProcessNode.strategyManager().errorTrigger(session);
            if (errorThrow == null || !errorThrow.isNode()) {
                throw FlowValidationException.nodeRequired("errorTrigger.node");
            }
            subProcessRecord.error();
            subProcessRepository.save(subProcessRecord);
            jumpToErrorNode(session, errorThrow.getNode());
        }
    }

    private FlowSession createParentSession(SubProcessRecord subProcessRecord) {
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
        FlowSession session = new FlowSession(repositoryHolder,
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
        session.setSubProcessContext(new SubProcessContext(subProcessRecord, childFinalRecord));
        return session;
    }

    private void resumeNextNodes(FlowSession session) {
        BaseAction action = (BaseAction) session.getCurrentAction();
        List<FlowRecord> records = new ArrayList<>();
        List<IFlowEvent> events = new ArrayList<>();
        action.triggerNode(session, triggerSession -> collectRecords(triggerSession, records, events));
        save(records, events);
    }

    private void jumpToErrorNode(FlowSession session, IFlowNode errorNode) {
        // 跳转目标为抄送/子流程节点时无法产生有效记录，直接拒绝而非静默停滞
        BaseFlowNode.verifyJumpTarget(errorNode);
        List<FlowRecord> records = new ArrayList<>();
        List<IFlowEvent> events = new ArrayList<>();
        collectRecords(session.updateSession(errorNode), records, events);
        save(records, events);
    }

    private void collectRecords(FlowSession session, List<FlowRecord> records, List<IFlowEvent> events) {
        List<FlowRecord> generated = session.getCurrentAction().generateRecords(session);
        records.addAll(generated);
        for (FlowRecord record : generated) {
            if (!record.isShow()) {
                continue;
            }
            if (record.isTodo() && !record.isNotify()) {
                events.add(new FlowRecordTodoEvent(record, session.isMock()));
            } else {
                events.add(new FlowRecordDoneEvent(record, session.isMock()));
            }
        }
    }

    private void save(List<FlowRecord> records, List<IFlowEvent> events) {
        if (!records.isEmpty()) {
            repositoryHolder.saveRecords(records);
        }
        events.forEach(EventPusher::push);
    }
}

package com.codingapi.flow.service.impl;

import com.codingapi.flow.action.BaseAction;
import com.codingapi.flow.domain.SubProcessContext;
import com.codingapi.flow.domain.SubProcessRecord;
import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.event.FlowRecordDoneEvent;
import com.codingapi.flow.event.FlowRecordTodoEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.SubProcessNode;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.SubProcessRepository;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.strategy.node.SubProcessStrategy;
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

    public FlowSubProcessResultService(FlowRecord childFinalRecord, IRepositoryHolder repositoryHolder) {
        this.childFinalRecord = childFinalRecord;
        this.repositoryHolder = repositoryHolder;
        this.subProcessRepository = repositoryHolder.getSubProcessRepository();
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
        FlowSession session = SubProcessSessionFactory.createParentSession(
                subProcessRecord, repositoryHolder, repositoryHolder.getWorkflowService());
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

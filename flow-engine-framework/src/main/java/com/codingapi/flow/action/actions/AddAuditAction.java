package com.codingapi.flow.action.actions;

import com.codingapi.flow.action.ActionDisplay;
import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.BaseAction;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.event.FlowRecordTodoEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.exception.FlowExecutionException;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.manager.NodeStrategyManager;
import com.codingapi.flow.manager.OperatorManager;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.node.OperatorLoadScript;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.springboot.framework.event.EventPusher;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 加签
 */
public class AddAuditAction extends BaseAction {

    public static final String DEFAULT_TITLE = "加签";

    /**
     * 可以加签的人员范围
     */
    private OperatorLoadScript script;

    /**
     * 最大可选人数，-1 表示不限制，正整数表示可选人员的最大数
     */
    private int maxOperatorCount = -1;


    public static AddAuditAction defaultAction() {
        AddAuditAction action = new AddAuditAction();
        action.setId(FlowIDGeneratorGatewayContext.getInstance().generateActionId());
        action.setTitle(DEFAULT_TITLE);
        action.setEnable(true);
        action.setType(ActionType.ADD_AUDIT.name());
        action.setDisplay(ActionDisplay.defaultDisplay(DEFAULT_TITLE));
        action.setScript(null);
        return action;
    }

    public void setScript(String script) {
        if (StringUtils.hasText(script)) {
            this.script = new OperatorLoadScript(script);
        }
    }

    public int getMaxOperatorCount() {
        return maxOperatorCount;
    }

    public void setMaxOperatorCount(int maxOperatorCount) {
        this.maxOperatorCount = maxOperatorCount;
    }

    /**
     * 加签的人员范围
     */
    public List<IFlowOperator> operators(FlowSession flowSession) {
        return script.execute(flowSession);
    }


    @Override
    public void copy(IFlowAction action) {
        super.copy(action);
        this.script = ((AddAuditAction) action).script;
        this.maxOperatorCount = ((AddAuditAction) action).maxOperatorCount;
    }


    @Override
    public void run(FlowSession flowSession) {
        IRepositoryHolder repositoryHolder = flowSession.getRepositoryHolder();
        List<IFlowEvent> flowEvents = new ArrayList<>();
        List<FlowRecord> flowRecords = new ArrayList<>();
        FlowRecord currentRecord = flowSession.getCurrentRecord();
        IFlowNode currentNode = flowSession.getCurrentNode();
        List<FlowRecord> currentRecords = flowSession.getCurrentNodeRecords();
        List<IFlowOperator> auditOperators = flowSession.getAdvice().getForwardOperators();
        if (maxOperatorCount >= 0 && auditOperators.size() > maxOperatorCount) {
            throw FlowExecutionException.operatorCountExceeded("addAudit", maxOperatorCount);
        }
        if (script != null) {
            OperatorManager operatorManager = new OperatorManager(script.execute(flowSession));
            for (IFlowOperator auditOperator : auditOperators) {
                if (!operatorManager.match(auditOperator)) {
                    throw FlowExecutionException.operatorNotInScope("addAudit");
                }
            }
        }
        // 已经在本节点待办队列中的人员不允许再次加签，避免同一审批人在同一节点重复产生待办
        verifyNotInTodoQueue(flowSession, auditOperators);

        int maxNodeOrder = 0;
        for (FlowRecord record : currentRecords) {
            if (record.getNodeOrder() >= maxNodeOrder) {
                maxNodeOrder = record.getNodeOrder();
            }
        }

        long fromId = currentRecord.getFromId();
        // 构建加签的记录
        for (IFlowOperator operator : auditOperators) {
            List<FlowRecord> records = currentNode.generateCurrentRecords(flowSession.updateSession(operator));
            for (FlowRecord record : records) {
                NodeStrategyManager nodeStrategyManager = currentNode.strategyManager();
                if (nodeStrategyManager.isSequenceMultiOperatorType()) {
                    record.resetAddAudit(fromId, ++maxNodeOrder, operator.getUserId(), true);
                } else {
                    record.resetAddAudit(fromId, ++maxNodeOrder, operator.getUserId(), false);
                }
            }
            flowRecords.addAll(records);
        }

        for (FlowRecord record : flowRecords) {
            if (record.isShow()) {
                flowEvents.add(new FlowRecordTodoEvent(record, flowSession.isMock()));
            }
        }

        repositoryHolder.saveRecords(flowRecords);
        flowEvents.forEach(EventPusher::push);
    }

    /**
     * 校验加签人员不能已经存在于当前节点的待办队列中。
     *
     * <p>待办队列指当前节点实例中尚未办理的审批记录：包括顺序审批里尚未激活的隐藏记录
     * （它们后续会按序号激活为待办），但不包括已办、已撤销以及已结束流程的记录。
     *
     * @param flowSession    当前会话
     * @param auditOperators 本次加签的人员
     */
    private void verifyNotInTodoQueue(FlowSession flowSession, List<IFlowOperator> auditOperators) {
        if (auditOperators == null || auditOperators.isEmpty()) {
            return;
        }
        Set<Long> pendingOperatorIds = pendingOperatorIds(flowSession);
        if (pendingOperatorIds.isEmpty()) {
            return;
        }
        for (IFlowOperator auditOperator : auditOperators) {
            if (pendingOperatorIds.contains(auditOperator.getUserId())) {
                throw FlowExecutionException.addAuditOperatorAlreadyInTodo(auditOperator.getUserId());
            }
        }
    }

    /**
     * 当前节点实例中仍有待办的人员id集合。
     *
     * <p>仓储按 {@code (fromId, nodeId)} 查询节点记录，开始节点（{@code fromId = 0}）等场景可能
     * 命中其他流程实例的记录，必须按 {@code processId} 限定，避免误判其他流程的待办人员。
     */
    private Set<Long> pendingOperatorIds(FlowSession flowSession) {
        FlowRecord currentRecord = flowSession.getCurrentRecord();
        List<FlowRecord> currentNodeRecords = flowSession.getCurrentNodeRecords();
        if (currentRecord == null || currentNodeRecords == null) {
            return Set.of();
        }
        return currentNodeRecords.stream()
                .filter(Objects::nonNull)
                .filter(record -> currentRecord.getProcessId().equals(record.getProcessId()))
                .filter(FlowRecord::isPendingApproval)
                .map(FlowRecord::getCurrentOperatorId)
                .collect(Collectors.toSet());
    }

    public static AddAuditAction fromMap(Map<String, Object> data) {
        AddAuditAction action = BaseAction.fromMap(data, AddAuditAction.class);
        String script = (String) data.get("script");
        action.setScript(script);
        Object maxOperatorCount = data.get("maxOperatorCount");
        if (maxOperatorCount != null) {
            action.setMaxOperatorCount(((Number) maxOperatorCount).intValue());
        }
        return action;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> data = super.toMap();
        if (script != null) {
            data.put("script", script.getScript());
        }
        // 仅在非默认值（-1）时写出 maxOperatorCount，历史数据保持向后兼容
        if (maxOperatorCount != -1) {
            data.put("maxOperatorCount", maxOperatorCount);
        }
        return data;
    }
}

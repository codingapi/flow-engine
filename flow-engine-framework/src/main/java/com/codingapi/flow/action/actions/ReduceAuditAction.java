package com.codingapi.flow.action.actions;

import com.codingapi.flow.action.ActionDisplay;
import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.BaseAction;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.event.FlowRecordReduceAuditEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.exception.FlowExecutionException;
import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.manager.OperatorManager;
import com.codingapi.flow.node.BaseAuditNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.node.OperatorLoadScript;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.strategy.node.MultiOperatorAuditStrategy;
import com.codingapi.springboot.framework.event.EventPusher;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 减签：由当前审批人把指定审批人从当前审批节点撤下（issue #266）。
 *
 * <p>与加签（{@link AddAuditAction}）对称：被减签人通过 {@code advice.forwardOperatorIds} 传入，
 * 可选人员范围通过动作的 {@code script} 限定。被减签的记录置为撤销，待办与合并关系由
 * {@link IRepositoryHolder#saveRecords(List)} 统一维护，不直接操作待办表。
 *
 * <p>仅支持循序（{@code SEQUENCE}）与并签（{@code MERGE}）两种多人审批模式：
 * 或签（{@code ANY}）与任意一人（{@code RANDOM_ONE}）单人通过即完成节点，减签没有业务意义。
 *
 * <p>减签不修改当前审批人的记录，当前审批人仍需通过「通过 / 拒绝」表达自己的审批意见。
 */
public class ReduceAuditAction extends BaseAction {

    public static final String DEFAULT_TITLE = "减签";

    /**
     * 可以减签的人员范围，为空表示不限
     */
    private OperatorLoadScript script;

    public static ReduceAuditAction defaultAction() {
        ReduceAuditAction action = new ReduceAuditAction();
        action.setId(FlowIDGeneratorGatewayContext.getInstance().generateActionId());
        action.setTitle(DEFAULT_TITLE);
        action.setEnable(true);
        action.setType(ActionType.REDUCE_AUDIT.name());
        action.setDisplay(ActionDisplay.defaultDisplay(DEFAULT_TITLE));
        action.setScript(null);
        return action;
    }

    public void setScript(String script) {
        if (StringUtils.hasText(script)) {
            this.script = new OperatorLoadScript(script);
        }
    }

    /**
     * 可以减签的人员范围
     */
    public List<IFlowOperator> operators(FlowSession flowSession) {
        return script.execute(flowSession);
    }

    @Override
    public void copy(IFlowAction action) {
        super.copy(action);
        this.script = ((ReduceAuditAction) action).script;
    }

    /**
     * 仅当当前节点存在可被减签的其他审批人时展示按钮。
     *
     * <p>单审批人节点、或签/任意一人节点、以及当前节点已无其他待审批人时不展示，
     * 避免出现点击后才被服务端拒绝的无效入口。
     */
    @Override
    public boolean show(FlowSession flowSession) {
        if (!super.show(flowSession)) {
            return false;
        }
        if (flowSession == null || !(flowSession.getCurrentNode() instanceof BaseAuditNode currentNode)) {
            return false;
        }
        if (!isSupportedMode(currentNode)) {
            return false;
        }
        return !pendingRecords(flowSession).isEmpty();
    }

    @Override
    public void run(FlowSession flowSession) {
        IRepositoryHolder repositoryHolder = flowSession.getRepositoryHolder();
        FlowRecord currentRecord = flowSession.getCurrentRecord();
        IFlowNode currentNode = flowSession.getCurrentNode();
        if (!(currentNode instanceof BaseAuditNode auditNode) || !isSupportedMode(currentNode)) {
            throw FlowExecutionException.reduceAuditUnsupported();
        }
        List<IFlowOperator> targets = flowSession.getAdvice().getForwardOperators();
        if (targets == null || targets.isEmpty()) {
            throw FlowValidationException.required("forwardOperators");
        }
        // 可减签人员范围
        if (script != null) {
            OperatorManager operatorManager = new OperatorManager(script.execute(flowSession));
            for (IFlowOperator target : targets) {
                if (!operatorManager.match(target)) {
                    throw FlowExecutionException.operatorNotInScope("reduceAudit");
                }
            }
        }

        List<FlowRecord> currentNodeRecords = currentNodeRecords(flowSession);
        List<FlowRecord> targetRecords = resolveTargetRecords(targets, currentNodeRecords, currentRecord);

        // 撤销后必须仍需要一次真实审批，否则节点条件即使满足也没有触发点，流程会永久卡死
        List<Long> targetRecordIds = targetRecords.stream().map(FlowRecord::getId).toList();
        List<FlowRecord> remainingRecords = currentNodeRecords.stream()
                .filter(record -> !targetRecordIds.contains(record.getId()))
                .toList();
        if (!auditNode.requiresAuditToFinish(remainingRecords)) {
            throw FlowExecutionException.reduceAuditNoRemainingApproval();
        }

        List<IFlowEvent> flowEvents = new ArrayList<>();
        List<FlowRecord> saveRecords = new ArrayList<>();
        for (FlowRecord targetRecord : targetRecords) {
            targetRecord.revoke();
            saveRecords.add(targetRecord);
            flowEvents.add(new FlowRecordReduceAuditEvent(targetRecord,
                    currentRecord.getCurrentOperatorId(), flowSession.isMock()));
        }

        // 循序审批的链路由通过时的「最近一条更大 nodeOrder」激活保证，撤销中间记录不需要额外补链
        repositoryHolder.saveRecords(saveRecords);
        flowEvents.forEach(EventPusher::push);
    }

    /**
     * 解析被减签的流程记录：目标必须是同一节点实例中尚未办理的审批记录。
     *
     * <p>同一审批人可能在该节点实例上持有多条待审批记录（例如加签会为其生成多条记录），
     * 减签按「人」生效，必须一并撤销，否则目标仍残留待办，等于没有减签。
     */
    private List<FlowRecord> resolveTargetRecords(List<IFlowOperator> targets,
                                                  List<FlowRecord> currentNodeRecords,
                                                  FlowRecord currentRecord) {
        List<FlowRecord> targetRecords = new ArrayList<>();
        for (IFlowOperator target : targets) {
            long targetOperatorId = target.getUserId();
            if (targetOperatorId == currentRecord.getCurrentOperatorId()) {
                throw FlowExecutionException.reduceAuditOwnRecord();
            }
            List<FlowRecord> matchedRecords = currentNodeRecords.stream()
                    .filter(this::isPendingApprovalRecord)
                    .filter(record -> record.getCurrentOperatorId() == targetOperatorId)
                    .toList();
            if (matchedRecords.isEmpty()) {
                throw FlowExecutionException.reduceAuditTargetNotFound(targetOperatorId);
            }
            for (FlowRecord matchedRecord : matchedRecords) {
                // 同一审批人被重复传入时按记录 id 去重
                if (targetRecords.stream().noneMatch(record -> record.getId() == matchedRecord.getId())) {
                    targetRecords.add(matchedRecord);
                }
            }
        }
        return targetRecords;
    }

    /**
     * 可减签的待办记录：当前节点实例中除自己以外的待审批记录（含顺序审批中尚未激活的隐藏记录）。
     */
    private List<FlowRecord> pendingRecords(FlowSession flowSession) {
        FlowRecord currentRecord = flowSession.getCurrentRecord();
        if (currentRecord == null) {
            return List.of();
        }
        return currentNodeRecords(flowSession).stream()
                .filter(this::isPendingApprovalRecord)
                .filter(record -> record.getId() != currentRecord.getId())
                .toList();
    }

    /**
     * 取当前节点实例中属于同一流程实例的记录。
     *
     * <p>仓储按 {@code (fromId, nodeId)} 查询，开始节点（{@code fromId = 0}）等场景可能命中
     * 其他流程实例的记录，必须按 {@code processId} 限定，避免误伤别的流程。
     */
    private List<FlowRecord> currentNodeRecords(FlowSession flowSession) {
        FlowRecord currentRecord = flowSession.getCurrentRecord();
        List<FlowRecord> currentNodeRecords = flowSession.getCurrentNodeRecords();
        if (currentRecord == null || currentNodeRecords == null) {
            return List.of();
        }
        return currentNodeRecords.stream()
                .filter(Objects::nonNull)
                .filter(record -> currentRecord.getProcessId().equals(record.getProcessId()))
                .toList();
    }

    private boolean isPendingApprovalRecord(FlowRecord record) {
        return !record.isRevoked()
                && record.getRecordState() == FlowRecord.SATE_RECORD_TODO
                && record.getFlowState() == FlowRecord.SATE_FLOW_RUNNING;
    }

    private boolean isSupportedMode(IFlowNode node) {
        MultiOperatorAuditStrategy.Type type = node.strategyManager().getMultiOperatorAuditStrategyType();
        return type == MultiOperatorAuditStrategy.Type.SEQUENCE
                || type == MultiOperatorAuditStrategy.Type.MERGE;
    }

    public static ReduceAuditAction fromMap(Map<String, Object> data) {
        ReduceAuditAction action = BaseAction.fromMap(data, ReduceAuditAction.class);
        action.setScript((String) data.get("script"));
        return action;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> data = super.toMap();
        if (script != null) {
            data.put("script", script.getScript());
        }
        return data;
    }
}

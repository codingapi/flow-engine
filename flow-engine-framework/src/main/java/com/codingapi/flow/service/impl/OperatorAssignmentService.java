package com.codingapi.flow.service.impl;

import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.operator.OperatorKey;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowAdvice;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.workflow.Workflow;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 操作人分配服务
 * <p>
 * 负责发起人设定 / 审批人设定时的操作人分配落库，并在节点配置了可选人员范围时校验所选人员是否越界。
 */
public class OperatorAssignmentService {

    private OperatorAssignmentService() {
    }

    /**
     * 判断指定节点是否已经设定操作人（issue #288 衍生 / #292）。
     *
     * <p>三种来源任一非空即视为已设定：
     * <ol>
     *     <li>本次意见携带的存量 {@code operatorSelectMap}；</li>
     *     <li>本次意见携带的 {@code operatorSelectOperators}（含身份）；</li>
     *     <li>此前（典型为发起时）已持久化的节点分配关系。</li>
     * </ol>
     * 只检查第 1 项会把「发起时预置的分配」和「按身份设定的分配」误判为未设定，
     * 使 {@code PassAction} 反复返回 OPERATOR_SELECT，流程停滞在开始节点。
     *
     * @param session 当前流程会话
     * @param nodeId  节点 ID
     * @return 是否已设定操作人
     */
    public static boolean hasAssignment(FlowSession session, String nodeId) {
        FlowAdvice advice = session.getAdvice();
        Map<String, List<Long>> idSelections = advice.getOperatorSelectMap();
        if (idSelections != null && idSelections.get(nodeId) != null && !idSelections.get(nodeId).isEmpty()) {
            return true;
        }
        Map<String, List<OperatorKey>> selections = advice.getOperatorSelectOperators();
        if (selections != null && selections.get(nodeId) != null && !selections.get(nodeId).isEmpty()) {
            return true;
        }
        FlowRecord currentRecord = session.getCurrentRecord();
        if (currentRecord == null || currentRecord.getProcessId() == null) {
            return false;
        }
        List<OperatorKey> assigned = session.getRepositoryHolder()
                .findAssignedOperators(currentRecord.getProcessId(), nodeId);
        return assigned != null && !assigned.isEmpty();
    }

    /**
     * 校验并保存操作人分配（仅人员 id，默认身份）。
     *
     * @param baseSession       基准会话，用于派生目标节点会话以执行范围脚本
     * @param processId         流程实例ID
     * @param operatorSelectMap 节点ID -> 选定的操作人ID列表
     */
    public static void validateAndSave(FlowSession baseSession, String processId,
                                       Map<String, List<Long>> operatorSelectMap) {
        if (operatorSelectMap == null || operatorSelectMap.isEmpty()) {
            return;
        }
        Map<String, List<OperatorKey>> selections = new LinkedHashMap<>();
        operatorSelectMap.forEach((nodeId, operatorIds) ->
                selections.put(nodeId, operatorIds.stream().map(OperatorKey::ofDefault).toList()));
        validateAndSaveOperators(baseSession, processId, selections);
    }

    /**
     * 校验并保存操作人分配，携带身份（issue #288 衍生 / #292）。
     * <p>
     * 数量校验：节点配置了最大可选人数（maxOperatorCount >= 0）时，所选人数超过上限则抛出异常。
     * 当目标节点配置了可选人员范围（范围非空）时，校验所选人员是否全部落在范围内，越界则抛出异常；
     * 范围为空（未配置脚本或脚本执行结果为空）表示不限范围，跳过校验。
     * <p>
     * 同一人员的两个身份是两项参与人，不做合并；完全相同的 {@code (人员id, 身份)} 会被去重。
     *
     * @param baseSession            基准会话，用于派生目标节点会话以执行范围脚本
     * @param processId              流程实例ID
     * @param operatorSelectOperators 节点ID -> 选定的操作人列表（含身份）
     */
    public static void validateAndSaveOperators(FlowSession baseSession, String processId,
                                                Map<String, List<OperatorKey>> operatorSelectOperators) {
        if (operatorSelectOperators == null || operatorSelectOperators.isEmpty()) {
            return;
        }
        Workflow workflow = baseSession.getWorkflow();
        IRepositoryHolder repositoryHolder = baseSession.getRepositoryHolder();
        operatorSelectOperators.forEach((nodeId, operators) -> {
            if (operators == null || operators.isEmpty()) {
                return;
            }
            List<OperatorKey> selections = operators.stream().distinct().toList();
            IFlowNode node = workflow.getFlowNode(nodeId);
            // 校验可选人数上限（-1 表示不限制）
            int maxOperatorCount = node.strategyManager().getMaxOperatorCount();
            if (maxOperatorCount >= 0 && selections.size() > maxOperatorCount) {
                throw FlowValidationException.operatorCountExceeded(nodeId, maxOperatorCount);
            }
            List<IFlowOperator> range = node.strategyManager()
                    .loadOperatorRange(baseSession.updateSession(node));
            if (range != null && !range.isEmpty()) {
                // 范围校验按人员 id 判定：范围表达的是「可选哪些人」，不区分身份
                Set<Long> allowedIds = range.stream()
                        .map(IFlowOperator::getUserId)
                        .collect(Collectors.toSet());
                for (OperatorKey operator : selections) {
                    if (!allowedIds.contains(operator.operatorId())) {
                        throw FlowValidationException.operatorOutOfRange(nodeId);
                    }
                }
            }
            repositoryHolder.saveOperatorAssignments(processId, nodeId, selections);
        });
    }
}

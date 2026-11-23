package com.codingapi.flow.strategy.node;

import com.codingapi.flow.common.IMapConvertor;
import com.codingapi.flow.manager.OperatorManager;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.operator.OperatorKey;
import com.codingapi.flow.script.node.OperatorLoadScript;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作人配置策略
 */
@NoArgsConstructor
public class OperatorLoadStrategy extends BaseStrategy {

    /**
     * 审批人配置脚本
     */
    private OperatorLoadScript operatorLoadScript;

    /**
     * 操作人选择方式，默认为脚本模式，保持向后兼容
     */
    @Getter
    private OperatorSelectType selectType = OperatorSelectType.SCRIPT;

    /**
     * 最大可选人数，-1 表示不限制，0 表示不允许选择，正整数表示可选人员的最大数
     */
    @Getter
    private int maxOperatorCount = -1;

    public OperatorLoadStrategy(String script) {
        this.operatorLoadScript = new OperatorLoadScript(script);
        this.selectType = OperatorSelectType.SCRIPT;
    }

    @Override
    public void copy(INodeStrategy target) {
        OperatorLoadStrategy t = (OperatorLoadStrategy) target;
        this.operatorLoadScript = t.operatorLoadScript;
        this.selectType = t.selectType;
        this.maxOperatorCount = t.maxOperatorCount;
    }


    public OperatorManager loadOperators(FlowSession flowSession) {
        if (selectType == OperatorSelectType.INITIATOR_SELECT
                || selectType == OperatorSelectType.APPROVER_SELECT) {
            // 从持久化存储中读取预先分配的操作人（含身份，issue #288 衍生 / #292）
            String processId = flowSession.getCurrentRecord() != null
                    ? flowSession.getCurrentRecord().getProcessId()
                    : null;
            String nodeId = flowSession.getCurrentNode().getId();
            if (processId != null) {
                List<OperatorKey> assignedOperators = flowSession.getRepositoryHolder()
                        .findAssignedOperators(processId, nodeId);
                if (!assignedOperators.isEmpty()) {
                    return new OperatorManager(this.loadAssignedOperators(
                            flowSession.getRepositoryHolder(), assignedOperators));
                }
            }
            // 未找到分配数据时返回空列表（触发 errorTrigger 逻辑）
            return new OperatorManager(List.of());
        }
        // 默认 SCRIPT 模式
        return new OperatorManager(operatorLoadScript.execute(flowSession));
    }

    /**
     * 按 {@code (人员id, 身份)} 加载已分配的操作人（issue #288 衍生 / #292）。
     *
     * <p>按身份分组批量回查，避免逐个 {@code getOperatorById} 造成 N+1；
     * 返回顺序与分配顺序一致，保证多人审批的执行顺序稳定。
     */
    private List<IFlowOperator> loadAssignedOperators(IRepositoryHolder repositoryHolder,
                                                      List<OperatorKey> assignedOperators) {
        Map<String, List<Long>> operatorIdsByIdentity = new LinkedHashMap<>();
        for (OperatorKey operator : assignedOperators) {
            operatorIdsByIdentity
                    .computeIfAbsent(operator.identity(), key -> new ArrayList<>())
                    .add(operator.operatorId());
        }
        Map<String, IFlowOperator> operatorMap = new HashMap<>();
        operatorIdsByIdentity.forEach((identity, operatorIds) -> {
            List<IFlowOperator> operators = repositoryHolder.findOperatorByIds(operatorIds, identity);
            if (operators == null) {
                return;
            }
            for (IFlowOperator operator : operators) {
                operatorMap.put(OperatorKey.of(operator.getUserId(), identity).key(), operator);
            }
        });
        List<IFlowOperator> result = new ArrayList<>(assignedOperators.size());
        for (OperatorKey operator : assignedOperators) {
            IFlowOperator flowOperator = operatorMap.get(operator.key());
            if (flowOperator != null) {
                result.add(flowOperator);
            }
        }
        return result;
    }

    /**
     * 计算该节点的可选人员范围（用于发起人/审批人设定模式）。
     * 复用 operatorLoadScript 执行脚本得到候选人；脚本为空或执行结果为空均视为不限范围（可选任意人）。
     *
     * @param flowSession 目标节点会话
     * @return 可选人员范围，返回空表示不限范围
     */
    public List<IFlowOperator> loadOperatorRange(FlowSession flowSession) {
        if (operatorLoadScript == null) {
            return List.of();
        }
        return operatorLoadScript.execute(flowSession);
    }

    public static OperatorLoadStrategy defaultStrategy() {
        OperatorLoadStrategy strategy = new OperatorLoadStrategy();
        strategy.operatorLoadScript = OperatorLoadScript.defaultScript();
        strategy.selectType = OperatorSelectType.SCRIPT;
        return strategy;
    }

    /**
     * 创建发起人设定策略（不限可选人员范围）
     */
    public static OperatorLoadStrategy initiatorSelectStrategy() {
        OperatorLoadStrategy strategy = new OperatorLoadStrategy();
        strategy.selectType = OperatorSelectType.INITIATOR_SELECT;
        return strategy;
    }

    /**
     * 创建发起人设定策略（带可选人员范围脚本）
     *
     * @param rangeScript 范围脚本，返回该节点的可选人员范围；为空表示不限范围
     */
    public static OperatorLoadStrategy initiatorSelectStrategy(String rangeScript) {
        OperatorLoadStrategy strategy = initiatorSelectStrategy();
        strategy.operatorLoadScript = new OperatorLoadScript(rangeScript);
        return strategy;
    }

    /**
     * 创建发起人设定策略（带可选人员范围脚本与可选人数上限）
     *
     * @param rangeScript     范围脚本，返回该节点的可选人员范围；为空表示不限范围
     * @param maxOperatorCount 最大可选人数，-1 表示不限制
     */
    public static OperatorLoadStrategy initiatorSelectStrategy(String rangeScript, int maxOperatorCount) {
        OperatorLoadStrategy strategy = initiatorSelectStrategy(rangeScript);
        strategy.maxOperatorCount = maxOperatorCount;
        return strategy;
    }

    /**
     * 创建审批人设定策略（不限可选人员范围）
     */
    public static OperatorLoadStrategy approverSelectStrategy() {
        OperatorLoadStrategy strategy = new OperatorLoadStrategy();
        strategy.selectType = OperatorSelectType.APPROVER_SELECT;
        return strategy;
    }

    /**
     * 创建审批人设定策略（带可选人员范围脚本）
     *
     * @param rangeScript 范围脚本，返回该节点的可选人员范围；为空表示不限范围
     */
    public static OperatorLoadStrategy approverSelectStrategy(String rangeScript) {
        OperatorLoadStrategy strategy = approverSelectStrategy();
        strategy.operatorLoadScript = new OperatorLoadScript(rangeScript);
        return strategy;
    }

    /**
     * 创建审批人设定策略（带可选人员范围脚本与可选人数上限）
     *
     * @param rangeScript     范围脚本，返回该节点的可选人员范围；为空表示不限范围
     * @param maxOperatorCount 最大可选人数，-1 表示不限制
     */
    public static OperatorLoadStrategy approverSelectStrategy(String rangeScript, int maxOperatorCount) {
        OperatorLoadStrategy strategy = approverSelectStrategy(rangeScript);
        strategy.maxOperatorCount = maxOperatorCount;
        return strategy;
    }

    /**
     * 设置最大可选人数
     *
     * @param maxOperatorCount 最大可选人数，-1 表示不限制
     */
    public void setMaxOperatorCount(int maxOperatorCount) {
        this.maxOperatorCount = maxOperatorCount;
    }


    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("selectType", selectType.name());
        // 仅在非默认值（-1）时写出 maxOperatorCount，历史数据保持向后兼容
        if (maxOperatorCount != -1) {
            map.put("maxOperatorCount", maxOperatorCount);
        }
        // SCRIPT 模式存审批人脚本；INITIATOR/APPROVER 模式存可选人员范围脚本
        if (operatorLoadScript != null) {
            map.put("script", operatorLoadScript.getScript());
        }
        return map;
    }

    public static OperatorLoadStrategy fromMap(Map<String, Object> map) {
        OperatorLoadStrategy strategy = IMapConvertor.fromMap(map, OperatorLoadStrategy.class);
        if (strategy == null) return null;
        String selectTypeStr = (String) map.get("selectType");
        if (selectTypeStr != null) {
            strategy.selectType = OperatorSelectType.valueOf(selectTypeStr);
        } else {
            // 向后兼容旧数据（没有 selectType 字段），默认为 SCRIPT
            strategy.selectType = OperatorSelectType.SCRIPT;
        }
        // 向后兼容旧数据（没有 maxOperatorCount 字段），默认 -1 表示不限制
        Object maxOperatorCount = map.get("maxOperatorCount");
        if (maxOperatorCount != null) {
            strategy.maxOperatorCount = ((Number) maxOperatorCount).intValue();
        }
        if (strategy.selectType == OperatorSelectType.SCRIPT) {
            strategy.operatorLoadScript = new OperatorLoadScript((String) map.get("script"));
        } else {
            // INITIATOR/APPROVER 模式：存在 script 时作为可选人员范围脚本，缺省表示不限范围
            String script = (String) map.get("script");
            if (script != null) {
                strategy.operatorLoadScript = new OperatorLoadScript(script);
            }
        }
        return strategy;
    }
}

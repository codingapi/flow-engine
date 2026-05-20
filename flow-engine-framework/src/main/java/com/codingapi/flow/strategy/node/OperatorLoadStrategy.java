package com.codingapi.flow.strategy.node;

import com.codingapi.flow.common.IMapConvertor;
import com.codingapi.flow.manager.OperatorManager;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.node.OperatorLoadScript;
import com.codingapi.flow.session.FlowSession;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    public OperatorLoadStrategy(String script) {
        this.operatorLoadScript = new OperatorLoadScript(script);
        this.selectType = OperatorSelectType.SCRIPT;
    }

    @Override
    public void copy(INodeStrategy target) {
        OperatorLoadStrategy t = (OperatorLoadStrategy) target;
        this.operatorLoadScript = t.operatorLoadScript;
        this.selectType = t.selectType;
    }


    public OperatorManager loadOperators(FlowSession flowSession) {
        if (selectType == OperatorSelectType.INITIATOR_SELECT
                || selectType == OperatorSelectType.APPROVER_SELECT) {
            // 从持久化存储中读取预先分配的操作人 ID 列表
            String processId = flowSession.getCurrentRecord() != null
                    ? flowSession.getCurrentRecord().getProcessId()
                    : null;
            String nodeId = flowSession.getCurrentNode().getId();
            if (processId != null) {
                List<Long> operatorIds = flowSession.getRepositoryHolder()
                        .findAssignedOperatorIds(processId, nodeId);
                if (!operatorIds.isEmpty()) {
                    List<IFlowOperator> operators = flowSession.getRepositoryHolder()
                            .findOperatorByIds(operatorIds);
                    return new OperatorManager(operators);
                }
            }
            // 未找到分配数据时返回空列表（触发 errorTrigger 逻辑）
            return new OperatorManager(List.of());
        }
        // 默认 SCRIPT 模式
        return new OperatorManager(operatorLoadScript.execute(flowSession));
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


    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("selectType", selectType.name());
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

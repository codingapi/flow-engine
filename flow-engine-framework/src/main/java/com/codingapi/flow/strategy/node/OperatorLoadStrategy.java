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

    public static OperatorLoadStrategy defaultStrategy() {
        OperatorLoadStrategy strategy = new OperatorLoadStrategy();
        strategy.operatorLoadScript = OperatorLoadScript.defaultScript();
        strategy.selectType = OperatorSelectType.SCRIPT;
        return strategy;
    }

    /**
     * 创建发起人设定策略
     */
    public static OperatorLoadStrategy initiatorSelectStrategy() {
        OperatorLoadStrategy strategy = new OperatorLoadStrategy();
        strategy.selectType = OperatorSelectType.INITIATOR_SELECT;
        return strategy;
    }

    /**
     * 创建审批人设定策略
     */
    public static OperatorLoadStrategy approverSelectStrategy() {
        OperatorLoadStrategy strategy = new OperatorLoadStrategy();
        strategy.selectType = OperatorSelectType.APPROVER_SELECT;
        return strategy;
    }


    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("selectType", selectType.name());
        if (selectType == OperatorSelectType.SCRIPT && operatorLoadScript != null) {
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
        }
        return strategy;
    }
}

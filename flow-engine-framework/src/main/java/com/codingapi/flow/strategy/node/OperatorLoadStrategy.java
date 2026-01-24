package com.codingapi.flow.strategy.node;

import com.codingapi.flow.common.IMapConvertor;
import com.codingapi.flow.node.manager.OperatorManager;
import com.codingapi.flow.script.node.OperatorLoadScript;
import com.codingapi.flow.session.FlowSession;
import lombok.NoArgsConstructor;

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

    public OperatorLoadStrategy(String script) {
        this.operatorLoadScript = new OperatorLoadScript(script);
    }

    @Override
    public void copy(INodeStrategy target) {
        this.operatorLoadScript = ((OperatorLoadStrategy) target).operatorLoadScript;
    }

    public void setOperatorLoadScript(String script) {
        this.operatorLoadScript = new OperatorLoadScript(script);
    }

    public OperatorManager loadOperators(FlowSession flowSession) {
        return new OperatorManager(operatorLoadScript.execute(flowSession));
    }

    public static OperatorLoadStrategy defaultStrategy() {
        OperatorLoadStrategy strategy = new OperatorLoadStrategy();
        strategy.setOperatorLoadScript(OperatorLoadScript.SCRIPT_CREATOR);
        return strategy;
    }


    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("script", operatorLoadScript.getScript());
        return map;
    }

    public static OperatorLoadStrategy fromMap(Map<String, Object> map) {
        OperatorLoadStrategy strategy = IMapConvertor.fromMap(map, OperatorLoadStrategy.class);
        if (strategy == null) return null;
        strategy.setOperatorLoadScript((String) map.get("script"));
        return strategy;
    }
}

package com.codingapi.flow.strategy;

import com.codingapi.flow.node.manager.OperatorManager;
import com.codingapi.flow.script.node.OperatorLoadScript;
import com.codingapi.flow.session.FlowSession;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
public class OperatorLoadStrategy implements INodeStrategy{

    /**
     * 审批人配置脚本
     */
    private OperatorLoadScript operatorLoadScript;

    public OperatorLoadStrategy(String script) {
        this.operatorLoadScript = new OperatorLoadScript(script);
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
        Map<String, Object> map = new HashMap<>();
        map.put(TYPE_KEY, strategyType());
        map.put("script", operatorLoadScript.getScript());
        return map;
    }

    public static OperatorLoadStrategy fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        OperatorLoadStrategy strategy = new OperatorLoadStrategy();
        strategy.setOperatorLoadScript((String) map.get("script"));
        return strategy;
    }
}

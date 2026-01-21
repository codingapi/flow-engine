package com.codingapi.flow.strategy;

import com.codingapi.flow.node.manager.OperatorManager;
import com.codingapi.flow.script.node.OperatorLoadScript;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

public class OperatorLoadStrategy extends BaseStrategy{

    /**
     * 审批人配置脚本
     */
    private OperatorLoadScript operatorLoadScript;

    public OperatorLoadStrategy() {
        super(RandomUtils.generateStringId());
    }

    public OperatorLoadStrategy(String script) {
        super(RandomUtils.generateStringId());
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
        OperatorLoadStrategy strategy = BaseStrategy.fromMap(map, OperatorLoadStrategy.class);
        if (strategy == null) return null;
        strategy.setOperatorLoadScript((String) map.get("script"));
        return strategy;
    }
}

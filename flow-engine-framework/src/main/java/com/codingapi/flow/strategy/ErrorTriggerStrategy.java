package com.codingapi.flow.strategy;

import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.script.node.ErrorTriggerScript;
import com.codingapi.flow.session.FlowSession;

import java.util.HashMap;
import java.util.Map;

public class ErrorTriggerStrategy implements INodeStrategy{

    private ErrorTriggerScript errorTriggerScript;

    public void setErrorTriggerScript(String script) {
        this.errorTriggerScript = new ErrorTriggerScript(script);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(TYPE_KEY, strategyType());
        map.put("script", errorTriggerScript.getScript());
        return map;
    }

    public static ErrorTriggerStrategy fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        ErrorTriggerStrategy strategy = new ErrorTriggerStrategy();
        strategy.setErrorTriggerScript((String) map.get("script"));
        return strategy;
    }

    public ErrorThrow errorTrigger(FlowSession flowSession) {
        return errorTriggerScript.execute(flowSession);
    }


    public static ErrorTriggerStrategy defaultStrategy() {
        ErrorTriggerStrategy strategy = new ErrorTriggerStrategy();
        strategy.setErrorTriggerScript(ErrorTriggerScript.SCRIPT_NODE_DEFAULT);
        return strategy;
    }

}

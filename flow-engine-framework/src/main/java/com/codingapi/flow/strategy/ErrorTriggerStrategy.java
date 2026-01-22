package com.codingapi.flow.strategy;

import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.script.node.ErrorTriggerScript;
import com.codingapi.flow.session.FlowSession;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 错误触发策略配置(没有匹配到人时)
 */
@NoArgsConstructor
public class ErrorTriggerStrategy extends BaseStrategy {

    private ErrorTriggerScript errorTriggerScript;

    public void setErrorTriggerScript(String script) {
        this.errorTriggerScript = new ErrorTriggerScript(script);
    }

    @Override
    public void copy(INodeStrategy target) {
        this.errorTriggerScript = ((ErrorTriggerStrategy) target).errorTriggerScript;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("script", errorTriggerScript.getScript());
        return map;
    }

    public static ErrorTriggerStrategy fromMap(Map<String, Object> map) {
        ErrorTriggerStrategy strategy = BaseStrategy.fromMap(map, ErrorTriggerStrategy.class);
        if (strategy == null) return null;
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

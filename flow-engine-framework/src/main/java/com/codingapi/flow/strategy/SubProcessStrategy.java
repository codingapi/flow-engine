package com.codingapi.flow.strategy;

import com.codingapi.flow.script.node.TriggerScript;
import com.codingapi.flow.session.FlowSession;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
public class SubProcessStrategy extends BaseStrategy{

    private TriggerScript triggerScript;

    public void setTriggerScript(String script) {
        this.triggerScript = new TriggerScript(script);
    }

    @Override
    public void copy(INodeStrategy target) {
        this.triggerScript = ((SubProcessStrategy) target).triggerScript;
    }

    public static SubProcessStrategy defaultStrategy() {
        SubProcessStrategy processStrategy = new SubProcessStrategy();
        processStrategy.setTriggerScript(TriggerScript.SCRIPT_DEFAULT);
        return processStrategy;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("script", triggerScript.getScript());
        return map;
    }

    public static SubProcessStrategy fromMap(Map<String, Object> map) {
        SubProcessStrategy processStrategy = BaseStrategy.fromMap(map, SubProcessStrategy.class);
        if (processStrategy == null) return null;
        processStrategy.triggerScript = new TriggerScript((String) map.get("script"));
        return processStrategy;
    }

    public void execute(FlowSession session) {
        triggerScript.execute(session);
    }
}

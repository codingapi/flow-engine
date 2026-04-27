package com.codingapi.flow.strategy.node;

import com.codingapi.flow.common.IMapConvertor;
import com.codingapi.flow.script.node.TriggerScript;
import com.codingapi.flow.session.FlowSession;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 触发策略配置
 */
@Getter
@NoArgsConstructor
public class TriggerStrategy extends BaseStrategy {

    private TriggerScript triggerScript;

    public TriggerStrategy(String triggerScript) {
        this.triggerScript = new TriggerScript(triggerScript);
    }

    @Override
    public void copy(INodeStrategy target) {
        this.triggerScript = ((TriggerStrategy) target).triggerScript;
    }

    public static TriggerStrategy defaultStrategy() {
        TriggerStrategy triggerStrategy = new TriggerStrategy();
        triggerStrategy.triggerScript = TriggerScript.defaultScript();
        return triggerStrategy;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("script", triggerScript.getScript());
        return map;
    }

    public static TriggerStrategy fromMap(Map<String, Object> map) {
        TriggerStrategy delayStrategy = IMapConvertor.fromMap(map, TriggerStrategy.class);
        if (delayStrategy == null) return null;
        delayStrategy.triggerScript = new TriggerScript((String) map.get("script"));
        return delayStrategy;
    }

    public void execute(FlowSession session) {
        triggerScript.execute(session);
    }
}

package com.codingapi.flow.strategy.workflow;

import com.codingapi.flow.common.IMapConvertor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 干预策略
 */
public class InterfereStrategy extends BaseStrategy {

    @Setter
    @Getter
    private boolean enable;

    public static InterfereStrategy defaultStrategy() {
        InterfereStrategy strategy = new InterfereStrategy();
        strategy.setEnable(true);
        return strategy;
    }

    @Override
    public void copy(IWorkflowStrategy target) {
        this.enable = ((InterfereStrategy) target).enable;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("enable", enable);
        return map;
    }

    public static InterfereStrategy fromMap(Map<String, Object> map) {
        InterfereStrategy strategy = IMapConvertor.fromMap(map, InterfereStrategy.class);
        if (strategy == null) return null;
        strategy.setEnable((boolean) map.get("enable"));
        return strategy;
    }

}

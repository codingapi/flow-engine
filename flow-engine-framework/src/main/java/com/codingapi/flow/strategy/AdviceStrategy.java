package com.codingapi.flow.strategy;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 节点审批意见策略
 */
@Data
public class AdviceStrategy implements INodeStrategy{

    /**
     * 是否可空
     */
    private boolean adviceNullable;
    /**
     * 是否可签名
     */
    private boolean signable;


    public static AdviceStrategy defaultStrategy() {
        AdviceStrategy strategy = new AdviceStrategy();
        strategy.setAdviceNullable(true);
        strategy.setSignable(false);
        return strategy;
    }


    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(TYPE_KEY, strategyType());
        map.put("adviceNullable", adviceNullable);
        map.put("signable", signable);
        return map;
    }

    public static AdviceStrategy fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        AdviceStrategy strategy = new AdviceStrategy();
        strategy.setAdviceNullable((boolean) map.get("adviceNullable"));
        strategy.setSignable((boolean) map.get("signable"));
        return strategy;
    }
}

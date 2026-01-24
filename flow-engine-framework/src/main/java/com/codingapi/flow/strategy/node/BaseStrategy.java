package com.codingapi.flow.strategy.node;

import java.util.HashMap;
import java.util.Map;

/**
 * 基础策略
 */
public abstract class BaseStrategy implements INodeStrategy {

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(TYPE_KEY, strategyType());
        return map;
    }

}

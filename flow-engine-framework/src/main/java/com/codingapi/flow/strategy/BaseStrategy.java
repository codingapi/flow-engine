package com.codingapi.flow.strategy;

import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.Map;

/**
 *  基础策略
 */
public abstract class BaseStrategy implements INodeStrategy {

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(TYPE_KEY, strategyType());
        return map;
    }

    @SneakyThrows
    public static <T extends BaseStrategy> T  fromMap(Map<String, Object> map, Class<T> clazz) {
        if (map == null || map.isEmpty()) return null;
        return clazz.getDeclaredConstructor().newInstance();
    }
}

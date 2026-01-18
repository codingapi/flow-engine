package com.codingapi.flow.strategy;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 记录合并策略
 */
@Data
public class RecordMergeStrategy implements INodeStrategy {

    private boolean mergeable;

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(TYPE_KEY, strategyType());
        map.put("mergeable", mergeable);
        return map;
    }

    public static RecordMergeStrategy fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        RecordMergeStrategy strategy = new RecordMergeStrategy();
        strategy.mergeable = (boolean) map.get("mergeable");
        return strategy;
    }

    public static RecordMergeStrategy defaultStrategy() {
        RecordMergeStrategy strategy = new RecordMergeStrategy();
        strategy.setMergeable(false);
        return strategy;
    }
}

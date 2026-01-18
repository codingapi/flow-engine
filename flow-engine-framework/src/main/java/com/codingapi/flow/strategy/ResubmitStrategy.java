package com.codingapi.flow.strategy;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 重新提交策略
 */
@Data
public class ResubmitStrategy implements INodeStrategy {

    private Type type;

    public enum Type {
        // 恢复到当前节点
        RESUME,
        // 逐级提交
        CHAIN,
    }

    public boolean isResume() {
        return type == Type.RESUME;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(TYPE_KEY, strategyType());
        map.put("type", type);
        return map;
    }

    public static ResubmitStrategy fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        ResubmitStrategy strategy = new ResubmitStrategy();
        strategy.setType(Type.valueOf((String) map.get("type")));
        return strategy;
    }

    public static ResubmitStrategy defaultStrategy() {
        ResubmitStrategy strategy = new ResubmitStrategy();
        strategy.setType(Type.RESUME);
        return strategy;
    }
}

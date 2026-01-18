package com.codingapi.flow.strategy;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 多人审批策略
 */
@Data
public class MultiOperatorAuditStrategy implements INodeStrategy{

    private Type type;
    // 并签比例(0~1)
    private float percent;

    public enum Type {
        // 循序提交
        SEQUENCE,
        // 合并审核(并签)
        MERGE,
        // 任意审核(或签)
        ANY,
        // 随机一人员审批
        RANDOM_ONE
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(TYPE_KEY, strategyType());
        map.put("type", type);
        map.put("percent", String.valueOf(percent));
        return map;
    }

    public static MultiOperatorAuditStrategy fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        MultiOperatorAuditStrategy strategy = new MultiOperatorAuditStrategy();
        strategy.setType(Type.valueOf((String) map.get("type")));
        strategy.setPercent(Float.parseFloat((String) map.get("percent")));
        return strategy;
    }

    public static MultiOperatorAuditStrategy defaultStrategy() {
        MultiOperatorAuditStrategy strategy = new MultiOperatorAuditStrategy();
        strategy.setType(Type.SEQUENCE);
        return strategy;
    }
}

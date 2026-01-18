package com.codingapi.flow.strategy;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 提交人与审批人一致
 */
@Data
public class SameOperatorAuditStrategy implements INodeStrategy {

    private Type type;

    public enum Type {
        // 自动审批
        AUTO_PASS,
        // 手动审批
        MANUAL_PASS,
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(TYPE_KEY, strategyType());
        map.put("type", type);
        return map;
    }

    public static SameOperatorAuditStrategy fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        SameOperatorAuditStrategy strategy = new SameOperatorAuditStrategy();
        strategy.setType(Type.valueOf((String) map.get("type")));
        return strategy;
    }

    public static SameOperatorAuditStrategy defaultStrategy() {
        SameOperatorAuditStrategy strategy = new SameOperatorAuditStrategy();
        strategy.setType(Type.AUTO_PASS);
        return strategy;
    }
}

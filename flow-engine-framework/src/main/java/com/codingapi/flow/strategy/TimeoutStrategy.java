package com.codingapi.flow.strategy;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 超时策略
 */
@Data
public class TimeoutStrategy implements INodeStrategy {

    // 默认超时时间,默认1天
    public static final long DEFAULT_TIMEOUT_TIME = 60 * 60 * 24 * 1000;

    private Type type;
    private long timeoutTime;

    public enum Type {
        // 自动提醒
        REMIND,
        // 自动同意
        PASS,
        // 自动拒绝
        REJECT,
    }

    public static TimeoutStrategy defaultStrategy() {
        TimeoutStrategy timeoutStrategy = new TimeoutStrategy();
        timeoutStrategy.setType(Type.REMIND);
        timeoutStrategy.setTimeoutTime(DEFAULT_TIMEOUT_TIME);
        return timeoutStrategy;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(TYPE_KEY, strategyType());
        map.put("type", type);
        map.put("timeoutTime", String.valueOf(timeoutTime));
        return map;
    }

    public static TimeoutStrategy fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        TimeoutStrategy timeoutStrategy = new TimeoutStrategy();
        timeoutStrategy.setType(Type.valueOf(map.get("type").toString()));
        timeoutStrategy.setTimeoutTime(Long.parseLong(map.get("timeoutTime").toString()));
        return timeoutStrategy;
    }
}

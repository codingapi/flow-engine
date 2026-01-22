package com.codingapi.flow.strategy;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 超时策略配置
 */
@Data
@NoArgsConstructor
public class TimeoutStrategy extends BaseStrategy {

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


    @Override
    public void copy(INodeStrategy target) {
        this.type = ((TimeoutStrategy) target).getType();
        this.timeoutTime = ((TimeoutStrategy) target).getTimeoutTime();
    }

    public static TimeoutStrategy defaultStrategy() {
        TimeoutStrategy timeoutStrategy = new TimeoutStrategy();
        timeoutStrategy.setType(Type.REMIND);
        timeoutStrategy.setTimeoutTime(DEFAULT_TIMEOUT_TIME);
        return timeoutStrategy;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("type", type);
        map.put("timeoutTime", String.valueOf(timeoutTime));
        return map;
    }

    public static TimeoutStrategy fromMap(Map<String, Object> map) {
        TimeoutStrategy timeoutStrategy = BaseStrategy.fromMap(map, TimeoutStrategy.class);
        if (timeoutStrategy == null) return null;
        timeoutStrategy.setType(Type.valueOf(map.get("type").toString()));
        timeoutStrategy.setTimeoutTime(Long.parseLong(map.get("timeoutTime").toString()));
        return timeoutStrategy;
    }
}

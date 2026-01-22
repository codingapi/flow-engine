package com.codingapi.flow.strategy;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * 延迟策略配置
 */
@Setter
@Getter
@NoArgsConstructor
public class DelayStrategy extends BaseStrategy {

    private Type type;
    private int time;

    public enum Type {
        // 秒
        SECOND,
        // 分钟
        MINUTE,
        // 小时
        HOUR,
        // 天
        DAY,
    }

    public long getTriggerTime() {
        return switch (type) {
            case MINUTE -> (long) time * 1000 * 60;
            case HOUR -> (long) time * 1000 * 60 * 60;
            case DAY -> (long) time * 1000 * 60 * 60 * 24;
            default -> time * 1000L;
        };
    }


    @Override
    public void copy(INodeStrategy target) {
        this.type = ((DelayStrategy) target).getType();
        this.time = ((DelayStrategy) target).getTime();
    }

    public static DelayStrategy defaultStrategy() {
        DelayStrategy timeoutStrategy = new DelayStrategy();
        timeoutStrategy.setType(Type.SECOND);
        timeoutStrategy.setTime(5);
        return timeoutStrategy;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("type", type);
        map.put("time", String.valueOf(time));
        return map;
    }

    public static DelayStrategy fromMap(Map<String, Object> map) {
        DelayStrategy delayStrategy = BaseStrategy.fromMap(map, DelayStrategy.class);
        if (delayStrategy == null) return null;
        delayStrategy.setType(Type.valueOf(map.get("type").toString()));
        delayStrategy.setTime(Integer.parseInt(map.get("time").toString()));
        return delayStrategy;
    }
}

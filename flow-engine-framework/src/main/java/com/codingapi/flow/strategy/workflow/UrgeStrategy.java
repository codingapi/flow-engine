package com.codingapi.flow.strategy.workflow;

import com.codingapi.flow.common.IMapConvertor;
import com.codingapi.flow.domain.UrgeInterval;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 催办策略
 */
public class UrgeStrategy extends BaseStrategy{

    // 默认的催办间隔 60秒
    public static final int DEFAULT_INTERVAL = 60;


    @Setter
    @Getter
    private boolean enable;

    /**
     * 催办间隔秒数 单位：秒
     */
    @Setter
    @Getter
    private int interval;

    public static UrgeStrategy defaultStrategy() {
        UrgeStrategy strategy = new UrgeStrategy();
        strategy.setEnable(true);
        strategy.setInterval(DEFAULT_INTERVAL);
        return strategy;
    }

    @Override
    public void copy(IWorkflowStrategy target) {
        this.enable = ((UrgeStrategy) target).enable;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("enable", enable);
        map.put("interval", String.valueOf(interval));
        return map;
    }

    public static UrgeStrategy fromMap(Map<String, Object> map) {
        UrgeStrategy strategy = IMapConvertor.fromMap(map, UrgeStrategy.class);
        if (strategy == null) return null;
        strategy.setEnable((boolean) map.get("enable"));
        strategy.setInterval(Integer.parseInt(map.get("interval").toString()));
        return strategy;
    }

    /**
     * 是否有催办
     */
    public boolean hasUrge(UrgeInterval urgeInterval) {
        long interval = this.interval * 1000L;
        return urgeInterval.getCreateTime() + interval <= System.currentTimeMillis();
    }
}

package com.codingapi.flow.strategy.node;

import com.codingapi.flow.common.IMapConvertor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * 记录合并策略配置
 */
@Setter
@Getter
@NoArgsConstructor
public class RecordMergeStrategy extends BaseStrategy {

    private boolean enable;

    @Override
    public void copy(INodeStrategy target) {
        this.enable = ((RecordMergeStrategy) target).enable;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("enable", enable);
        return map;
    }

    public static RecordMergeStrategy fromMap(Map<String, Object> map) {
        RecordMergeStrategy strategy = IMapConvertor.fromMap(map, RecordMergeStrategy.class);
        if (strategy == null) return null;
        strategy.enable = (boolean) map.get("enable");
        return strategy;
    }

    public static RecordMergeStrategy defaultStrategy() {
        RecordMergeStrategy strategy = new RecordMergeStrategy();
        strategy.setEnable(false);
        return strategy;
    }
}

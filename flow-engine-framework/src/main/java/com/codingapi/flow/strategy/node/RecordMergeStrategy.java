package com.codingapi.flow.strategy.node;

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

    private boolean mergeable;

    @Override
    public void copy(INodeStrategy target) {
        this.mergeable = ((RecordMergeStrategy) target).mergeable;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("mergeable", mergeable);
        return map;
    }

    public static RecordMergeStrategy fromMap(Map<String, Object> map) {
        RecordMergeStrategy strategy = BaseStrategy.fromMap(map, RecordMergeStrategy.class);
        if (strategy == null) return null;
        strategy.mergeable = (boolean) map.get("mergeable");
        return strategy;
    }

    public static RecordMergeStrategy defaultStrategy() {
        RecordMergeStrategy strategy = new RecordMergeStrategy();
        strategy.setMergeable(false);
        return strategy;
    }
}

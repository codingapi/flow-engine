package com.codingapi.flow.strategy;

import com.codingapi.flow.utils.RandomUtils;
import lombok.Data;

import java.util.Map;

/**
 * 记录合并策略
 */
@Data
public class RecordMergeStrategy extends BaseStrategy {

    private boolean mergeable;

    public RecordMergeStrategy() {
        super(RandomUtils.generateStringId());
    }

    @Override
    public void copy(INodeStrategy target) {
        this.mergeable = ((RecordMergeStrategy)target).mergeable;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("mergeable", mergeable);
        return map;
    }

    public static RecordMergeStrategy fromMap(Map<String, Object> map) {
        RecordMergeStrategy strategy = BaseStrategy.fromMap( map, RecordMergeStrategy.class);
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

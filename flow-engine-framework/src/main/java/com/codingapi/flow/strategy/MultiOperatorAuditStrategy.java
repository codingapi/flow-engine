package com.codingapi.flow.strategy;

import com.codingapi.flow.utils.RandomUtils;
import lombok.Data;

import java.util.Map;

/**
 * 多人审批策略
 */
@Data
public class MultiOperatorAuditStrategy extends BaseStrategy {

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

    public MultiOperatorAuditStrategy() {
        super(RandomUtils.generateStringId());
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("type", type);
        map.put("percent", String.valueOf(percent));
        return map;
    }

    public static MultiOperatorAuditStrategy fromMap(Map<String, Object> map) {
        MultiOperatorAuditStrategy strategy = BaseStrategy.fromMap(map, MultiOperatorAuditStrategy.class);
        if (strategy == null) return null;
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

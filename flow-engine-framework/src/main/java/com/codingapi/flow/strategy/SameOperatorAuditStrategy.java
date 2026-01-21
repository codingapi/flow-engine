package com.codingapi.flow.strategy;

import com.codingapi.flow.utils.RandomUtils;
import lombok.Data;

import java.util.Map;

/**
 * 提交人与审批人一致
 */
@Data
public class SameOperatorAuditStrategy extends BaseStrategy  {

    private Type type;

    public enum Type {
        // 自动审批
        AUTO_PASS,
        // 手动审批
        MANUAL_PASS,
    }

    public SameOperatorAuditStrategy() {
        super(RandomUtils.generateStringId());
    }

    @Override
    public void copy(INodeStrategy target) {
        this.type = ((SameOperatorAuditStrategy) target).getType();
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("type", type);
        return map;
    }

    public static SameOperatorAuditStrategy fromMap(Map<String, Object> map) {
        SameOperatorAuditStrategy strategy = BaseStrategy.fromMap(map, SameOperatorAuditStrategy.class);
        if (strategy == null) return null;
        strategy.setType(Type.valueOf((String) map.get("type")));
        return strategy;
    }

    public static SameOperatorAuditStrategy defaultStrategy() {
        SameOperatorAuditStrategy strategy = new SameOperatorAuditStrategy();
        strategy.setType(Type.AUTO_PASS);
        return strategy;
    }
}

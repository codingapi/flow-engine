package com.codingapi.flow.strategy.node;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * 重新提交策略配置
 */
@Setter
@Getter
@NoArgsConstructor
public class ResubmitStrategy extends BaseStrategy {

    private Type type;

    public enum Type {
        // 恢复到当前节点
        RESUME,
        // 逐级提交
        CHAIN,
    }


    @Override
    public void copy(INodeStrategy target) {
        this.type = ((ResubmitStrategy) target).type;
    }

    public boolean isResume() {
        return type == Type.RESUME;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("type", type);
        return map;
    }

    public static ResubmitStrategy fromMap(Map<String, Object> map) {
        ResubmitStrategy strategy = BaseStrategy.fromMap(map, ResubmitStrategy.class);
        if (strategy == null) return null;
        strategy.setType(Type.valueOf((String) map.get("type")));
        return strategy;
    }

    public static ResubmitStrategy defaultStrategy() {
        ResubmitStrategy strategy = new ResubmitStrategy();
        strategy.setType(Type.RESUME);
        return strategy;
    }
}

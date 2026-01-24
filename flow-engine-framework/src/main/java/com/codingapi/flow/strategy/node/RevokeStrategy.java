package com.codingapi.flow.strategy.node;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 撤回策略
 */
public class RevokeStrategy extends BaseStrategy{


    /**
     * 是否启用
     */
    @Setter
    @Getter
    private boolean enable;

    /**
     * 撤回类型
     */
    @Setter
    @Getter
    private Type type;


    /**
     * 是否撤回上级
     */
    public boolean isRemoveNext() {
        return type == Type.REVOKE_NEXT;
    }


    public static enum Type{
        /**
         * 撤回下级审批
         */
        REVOKE_NEXT,
        /**
         * 撤回到当前节点
         */
        REVOKE_CURRENT
    }

    public static RevokeStrategy defaultStrategy() {
        RevokeStrategy strategy = new RevokeStrategy();
        strategy.setType(Type.REVOKE_CURRENT);
        strategy.setEnable(true);
        return strategy;
    }

    public static RevokeStrategy fromMap(Map<String, Object> map) {
        RevokeStrategy strategy = BaseStrategy.fromMap(map, RevokeStrategy.class);
        if (strategy == null) return null;
        strategy.setEnable(Boolean.parseBoolean(map.get("enable").toString()));
        strategy.setType(Type.valueOf(map.get("type").toString()));
        return strategy;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("enable", enable);
        map.put("type", type);
        return map;
    }

    @Override
    public void copy(INodeStrategy target) {
        this.enable = ((RevokeStrategy) target).enable;
        this.type = ((RevokeStrategy) target).type;
    }
}

package com.codingapi.flow.strategy.node;

import java.util.Map;

/**
 * 节点配置策略
 */
public interface INodeStrategy {

    String TYPE_KEY = "strategyType";

    Map<String, Object> toMap();


    default String strategyType() {
        return this.getClass().getSimpleName();
    }

    void copy(INodeStrategy target);

}

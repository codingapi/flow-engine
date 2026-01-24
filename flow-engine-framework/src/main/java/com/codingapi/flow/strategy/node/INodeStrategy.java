package com.codingapi.flow.strategy.node;

import com.codingapi.flow.convert.IMapConvertor;

/**
 * 节点配置策略
 */
public interface INodeStrategy extends IMapConvertor {

    String TYPE_KEY = "strategyType";


    default String strategyType() {
        return this.getClass().getSimpleName();
    }

    void copy(INodeStrategy target);

}

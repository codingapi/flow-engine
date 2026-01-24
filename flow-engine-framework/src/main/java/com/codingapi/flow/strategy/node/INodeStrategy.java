package com.codingapi.flow.strategy.node;

import com.codingapi.flow.common.ICopyAbility;
import com.codingapi.flow.common.IMapConvertor;

/**
 * 节点配置策略
 */
public interface INodeStrategy extends IMapConvertor, ICopyAbility<INodeStrategy> {

    String TYPE_KEY = "strategyType";


    default String strategyType() {
        return this.getClass().getSimpleName();
    }

}

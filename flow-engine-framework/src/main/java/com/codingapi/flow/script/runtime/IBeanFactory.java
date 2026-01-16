package com.codingapi.flow.script.runtime;

import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.operator.IFlowOperator;

import java.util.List;

public interface IBeanFactory {

    <T> T getBean(Class<T> clazz);

    <T> T getBean(String name, Class<T> clazz);

    <T> List<T> getBeans(Class<T> clazz);

    default IFlowOperator getOperatorById(long userId){
        return GatewayContext.getInstance().getFlowOperator(userId);
    }

    default List<IFlowOperator> findByIds(long ... ids){
        return GatewayContext.getInstance().findByIds(ids);
    }

    default List<IFlowOperator> findByIds(List<Long> ids){
        return GatewayContext.getInstance().findByIds(ids);
    }
}

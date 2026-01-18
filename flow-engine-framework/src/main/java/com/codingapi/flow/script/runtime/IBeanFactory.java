package com.codingapi.flow.script.runtime;

import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;

import java.util.List;

public interface IBeanFactory {

    default <T> T getBean(Class<T> clazz) {
        return null;
    }

    default <T> T getBean(String name, Class<T> clazz) {
        return null;
    }

    default <T> List<T> getBeans(Class<T> clazz) {
        return null;
    }

    default FlowRecord getRecordById(long id) {
        return null;
    }

    default IFlowOperator getOperatorById(long userId) {
        return GatewayContext.getInstance().getFlowOperator(userId);
    }

    default List<IFlowOperator> findOperatorsByIds(long... ids) {
        return GatewayContext.getInstance().findByIds(ids);
    }

    default List<IFlowOperator> findOperatorsByIds(List<Long> ids) {
        return GatewayContext.getInstance().findByIds(ids);
    }

}

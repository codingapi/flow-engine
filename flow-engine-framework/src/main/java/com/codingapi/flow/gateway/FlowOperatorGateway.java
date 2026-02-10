package com.codingapi.flow.gateway;

import com.codingapi.flow.operator.IFlowOperator;

import java.util.List;

/**
 * 流程操作者防腐层
 */
public interface FlowOperatorGateway {

    IFlowOperator get(long id);

    List<IFlowOperator> findByIds(List<Long> ids);

}

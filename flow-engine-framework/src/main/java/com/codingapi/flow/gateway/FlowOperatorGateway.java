package com.codingapi.flow.gateway;

import com.codingapi.flow.operator.IFlowOperator;

import java.util.List;

public interface FlowOperatorGateway {

    IFlowOperator get(long id);

    List<IFlowOperator> findByIds(List<Long> ids);

}

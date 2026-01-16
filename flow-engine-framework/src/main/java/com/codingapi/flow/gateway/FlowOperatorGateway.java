package com.codingapi.flow.gateway;

import com.codingapi.flow.operator.IFlowOperator;

public interface FlowOperatorGateway {

    IFlowOperator get(long id);

}

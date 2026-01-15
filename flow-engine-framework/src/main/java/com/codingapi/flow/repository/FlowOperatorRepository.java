package com.codingapi.flow.repository;

import com.codingapi.flow.user.IFlowOperator;

public interface FlowOperatorRepository {

    IFlowOperator get(long id);
}

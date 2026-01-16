package com.codingapi.flow.operator;

import lombok.Getter;

import java.util.List;

public class NodeOperators {

    @Getter
    private final List<IFlowOperator> operators;
    private final List<Long> operatorIds;

    public NodeOperators(List<IFlowOperator> operators) {
        this.operators = operators;
        this.operatorIds = operators.stream().map(IFlowOperator::getUserId).toList();
    }

    public boolean match(IFlowOperator operator){
        return operatorIds.contains(operator.getUserId());
    }
}

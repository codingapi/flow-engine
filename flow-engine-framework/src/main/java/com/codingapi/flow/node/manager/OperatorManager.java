package com.codingapi.flow.node.manager;

import com.codingapi.flow.operator.IFlowOperator;
import lombok.Getter;

import java.util.List;

/**
 *  节点操作者管理
 */
public class OperatorManager {

    @Getter
    private final List<IFlowOperator> operators;
    private final List<Long> operatorIds;

    public OperatorManager(List<IFlowOperator> operators) {
        this.operators = operators;
        this.operatorIds = operators.stream().map(IFlowOperator::getUserId).toList();
    }

    public boolean match(IFlowOperator operator) {
        return operatorIds.contains(operator.getUserId());
    }
}

package com.codingapi.flow.manager;

import com.codingapi.flow.operator.IFlowOperator;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 节点操作者管理
 */
public class OperatorManager {

    @Getter
    private final List<IFlowOperator> operators;
    private final List<Long> operatorIds;

    public OperatorManager(List<IFlowOperator> operators) {
        if(operators!=null && !operators.isEmpty()) {
            this.operators = operators.stream().filter(Objects::nonNull).toList();
        }else {
            this.operators = new ArrayList<>();
        }
        if(!this.operators.isEmpty()) {
            this.operatorIds = this.operators.stream().map(IFlowOperator::getUserId).toList();
        }else {
            this.operatorIds = new ArrayList<>();
        }
    }

    public boolean isEmpty() {
        return operators.isEmpty();
    }

    public boolean match(IFlowOperator operator) {
        return operatorIds.contains(operator.getUserId());
    }


    public IFlowOperator getOperator(long userId) {
        for (IFlowOperator operator : operators) {
            if (operator.getUserId() == userId) {
                return operator;
            }
        }
        return null;
    }
}

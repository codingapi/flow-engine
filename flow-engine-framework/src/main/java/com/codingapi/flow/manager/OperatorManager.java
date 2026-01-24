package com.codingapi.flow.manager;

import com.codingapi.flow.operator.IFlowOperator;
import lombok.Getter;

import java.util.List;

/**
 * 节点操作者管理
 */
public class OperatorManager {

    @Getter
    private final List<IFlowOperator> operators;
    private final List<Long> operatorIds;

    public OperatorManager(List<IFlowOperator> operators) {
        this.operators = operators.stream().map(this::fetchForwardOperator).toList();
        this.operatorIds = operators.stream().map(IFlowOperator::getUserId).toList();
    }


    /**
     * 获取委托之后的真正操作者
     *
     * @param operator 操作者
     * @return 真正的操作者
     */
    private IFlowOperator fetchForwardOperator(IFlowOperator operator) {
        if (operator.forwardOperator() != null) {
            return fetchForwardOperator(operator.forwardOperator());
        }
        return operator;
    }

    public boolean match(IFlowOperator operator) {
        return operatorIds.contains(operator.getUserId());
    }
}

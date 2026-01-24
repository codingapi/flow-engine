package com.codingapi.flow.manager;

import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.strategy.workflow.IWorkflowStrategy;
import com.codingapi.flow.strategy.workflow.InterfereStrategy;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 流程策略管理
 */
@AllArgsConstructor
public class WorkflowStrategyManager {

    private final List<IWorkflowStrategy> strategies;


    public boolean isEnableInterfere() {
        InterfereStrategy interfereStrategy = getStrategy(InterfereStrategy.class);
        if (interfereStrategy != null) {
            return interfereStrategy.isEnable();
        }
        return false;
    }

    public boolean isEnableUrge() {
        InterfereStrategy interfereStrategy = getStrategy(InterfereStrategy.class);
        if (interfereStrategy != null) {
            return interfereStrategy.isEnable();
        }
        return false;
    }


    @SuppressWarnings("unchecked")
    public <T extends IWorkflowStrategy> T getStrategy(Class<T> clazz) {
        for (IWorkflowStrategy strategy : strategies) {
            if (strategy.getClass() == clazz) {
                return (T) strategy;
            }
        }
        return null;
    }

    /**
     * 验证操作者
     *
     * @param currentOperator  当前操作者
     * @param recordOperatorId 记录操作者id
     */
    public void verifyOperator(IFlowOperator currentOperator, long recordOperatorId) {
        if (!this.isEnableInterfere()) {
            if (recordOperatorId != currentOperator.getUserId()) {
                throw FlowStateException.operatorNotMatch();
            }
        } else {
            if (!currentOperator.isFlowManager()) {
                if (recordOperatorId != currentOperator.getUserId()) {
                    throw FlowStateException.operatorNotMatch();
                }
            }
        }
    }
}

package com.codingapi.flow.manager;

import com.codingapi.flow.strategy.workflow.IWorkflowStrategy;
import com.codingapi.flow.strategy.workflow.InterfereStrategy;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 *  流程策略管理
 */
@AllArgsConstructor
public class WorkflowStrategyManager {

    private final List<IWorkflowStrategy> strategies;


    public boolean isEnableInterfere() {
        InterfereStrategy interfereStrategy = getStrategy(InterfereStrategy.class);
        if(interfereStrategy!=null){
            return interfereStrategy.isEnable();
        }
        return false;
    }

    public boolean isEnableUrge() {
        InterfereStrategy interfereStrategy = getStrategy(InterfereStrategy.class);
        if(interfereStrategy!=null){
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
}

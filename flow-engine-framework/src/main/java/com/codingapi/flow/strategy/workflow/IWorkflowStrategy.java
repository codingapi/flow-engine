package com.codingapi.flow.strategy.workflow;

import com.codingapi.flow.common.ICopyAbility;
import com.codingapi.flow.common.IMapConvertor;

/**
 *  工作流策略
 */
public interface IWorkflowStrategy extends IMapConvertor, ICopyAbility<IWorkflowStrategy> {

    String TYPE_KEY = "strategyType";

    default String strategyType() {
        return this.getClass().getSimpleName();
    }
}

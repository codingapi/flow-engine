package com.codingapi.flow.builder;

import com.codingapi.flow.strategy.workflow.IWorkflowStrategy;

import java.util.ArrayList;
import java.util.List;

public class WorkflowStrategyBuilder {

    private final List<IWorkflowStrategy> strategies;

    private WorkflowStrategyBuilder() {
        this.strategies = new ArrayList<>();
    }

    public static WorkflowStrategyBuilder builder() {
        return new WorkflowStrategyBuilder();
    }

    public WorkflowStrategyBuilder addStrategy(IWorkflowStrategy strategy) {
        this.strategies.add(strategy);
        return this;
    }

    public List<IWorkflowStrategy> build() {
        return strategies;
    }
}

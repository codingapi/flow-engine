package com.codingapi.flow.builder;

import com.codingapi.flow.strategy.node.INodeStrategy;

import java.util.ArrayList;
import java.util.List;

public class NodeStrategyBuilder {

    private final List<INodeStrategy> strategies;

    private NodeStrategyBuilder() {
        this.strategies = new ArrayList<>();
    }

    public static NodeStrategyBuilder builder() {
        return new NodeStrategyBuilder();
    }

    public NodeStrategyBuilder addStrategy(INodeStrategy strategy) {
        this.strategies.add(strategy);
        return this;
    }

    public List<INodeStrategy> build() {
        return strategies;
    }
}

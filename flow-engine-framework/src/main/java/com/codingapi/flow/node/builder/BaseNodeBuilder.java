package com.codingapi.flow.node.builder;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.strategy.INodeStrategy;

import java.util.List;

public abstract class BaseNodeBuilder<B extends BaseNodeBuilder<B, N>, N extends BaseFlowNode> {

    protected final N node;

    public BaseNodeBuilder(N node) {
        this.node = node;
    }

    public B id(String id) {
        node.setId(id);
        return (B) this;
    }

    public B actions(List<IFlowAction> actions) {
        node.setActions(actions);
        return (B) this;
    }

    public B name(String name) {
        node.setName(name);
        return (B) this;
    }

    public B strategies(List<INodeStrategy> nodeStrategies) {
        node.setStrategies(nodeStrategies);
        return (B) this;
    }


    public B order(int order) {
        node.setOrder(order);
        return (B) this;
    }


    public N build() {
        return node;
    }
}

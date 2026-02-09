package com.codingapi.flow.builder;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.strategy.node.INodeStrategy;

import java.util.ArrayList;
import java.util.Arrays;
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

    public B blocks(IFlowNode... nodes) {
        List<IFlowNode> nodeList = new ArrayList<>(Arrays.asList(nodes));
        node.setBlocks(nodeList);
        return (B) this;
    }

    public B blocks(List<IFlowNode> nodes) {
        node.setBlocks(nodes);
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

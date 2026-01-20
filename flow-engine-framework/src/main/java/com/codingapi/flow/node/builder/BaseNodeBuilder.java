package com.codingapi.flow.node.builder;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.node.BaseFlowNode;

import java.util.List;

public abstract class BaseNodeBuilder<B extends BaseNodeBuilder<B, N>, N extends BaseFlowNode> {

    private final N node;

    public BaseNodeBuilder(N node) {
        this.node = node;
    }

    public B id(String id) {
        node.setId(id);
        return (B)this;
    }

    public B actions(List<IFlowAction> actions) {
        node.setActions(actions);
        return (B)this;
    }

    public B name(String name) {
        node.setName(name);
        return (B)this;
    }

    public N build() {
        return node;
    }
}

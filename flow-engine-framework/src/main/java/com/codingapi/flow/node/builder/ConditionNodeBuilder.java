package com.codingapi.flow.node.builder;

import com.codingapi.flow.node.BaseConditionNode;

public abstract class ConditionNodeBuilder<B extends ConditionNodeBuilder<B, N>, N extends BaseConditionNode> {

    protected final N node;

    public ConditionNodeBuilder(N node) {
        this.node = node;
    }

    public B id(String id) {
        node.setId(id);
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

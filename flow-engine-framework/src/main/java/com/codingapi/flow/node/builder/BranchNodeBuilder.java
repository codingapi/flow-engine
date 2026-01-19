package com.codingapi.flow.node.builder;

import com.codingapi.flow.node.BaseBranchNode;

public abstract class BranchNodeBuilder<B extends BranchNodeBuilder<B, N>, N extends BaseBranchNode> {

    protected final N node;

    public BranchNodeBuilder(N node) {
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

    public B order(int order) {
        node.setOrder(order);
        return (B)this;
    }
    public N build() {
        return node;
    }

}

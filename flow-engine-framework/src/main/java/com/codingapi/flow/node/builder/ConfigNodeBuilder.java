package com.codingapi.flow.node.builder;

import com.codingapi.flow.node.BaseConfigNode;

public abstract class ConfigNodeBuilder<B extends ConfigNodeBuilder<B, N>, N extends BaseConfigNode> {

    protected final N node;

    public ConfigNodeBuilder(N node) {
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

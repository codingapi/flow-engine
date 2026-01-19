package com.codingapi.flow.node.config;

import com.codingapi.flow.node.BaseConfigNode;
import com.codingapi.flow.node.builder.ConfigNodeBuilder;
import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

/**
 * 延迟节点
 */
public class DelayNode extends BaseConfigNode {

    public static final String NODE_TYPE = "delay";
    public static final String DEFAULT_NAME = "延迟节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }


    public DelayNode(String id, String name) {
        super(id, name);
    }

    public DelayNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME);
    }

    public static DelayNode formMap(Map<String, Object> map) {
        return BaseConfigNode.formMap(map, DelayNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ConfigNodeBuilder<Builder,DelayNode> {
        public Builder() {
            super(new DelayNode());
        }
    }
}

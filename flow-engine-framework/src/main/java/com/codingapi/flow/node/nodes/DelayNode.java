package com.codingapi.flow.node.nodes;

import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

/**
 * 延迟节点
 */
public class DelayNode extends BaseFlowNode {

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
        return BaseFlowNode.loadFromMap(map, DelayNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, DelayNode> {
        public Builder() {
            super(new DelayNode());
        }
    }
}

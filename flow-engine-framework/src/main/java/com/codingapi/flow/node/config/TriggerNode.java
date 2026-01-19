package com.codingapi.flow.node.config;

import com.codingapi.flow.node.BaseConfigNode;
import com.codingapi.flow.node.builder.ConfigNodeBuilder;
import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

/**
 * 触发节点
 */
public class TriggerNode extends BaseConfigNode {

    public static final String NODE_TYPE = "trigger";
    public static final String DEFAULT_NAME = "触发节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public TriggerNode(String id, String name) {
        super(id, name);
    }

    public TriggerNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME);
    }

    public static TriggerNode formMap(Map<String, Object> map) {
        return BaseConfigNode.formMap(map, TriggerNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ConfigNodeBuilder<Builder,TriggerNode> {
        public Builder() {
            super(new TriggerNode());
        }
    }
}

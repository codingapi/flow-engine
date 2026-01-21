package com.codingapi.flow.node.nodes;

import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.builder.BaseNodeBuilder;
import com.codingapi.flow.utils.RandomUtils;

import java.util.ArrayList;
import java.util.Map;

/**
 * 触发节点
 */
public class TriggerNode extends BaseFlowNode {

    public static final String NODE_TYPE = "trigger";
    public static final String DEFAULT_NAME = "触发节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public TriggerNode(String id, String name) {
        super(id, name,new ArrayList<>());
    }

    public TriggerNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME);
    }

    public static TriggerNode formMap(Map<String, Object> map) {
        return BaseFlowNode.loadFromMap(map, TriggerNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder,TriggerNode> {
        public Builder() {
            super(new TriggerNode());
        }
    }
}

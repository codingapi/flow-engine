package com.codingapi.flow.node.nodes;

import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.utils.RandomUtils;

import java.util.ArrayList;
import java.util.Map;

/**
 * 子流程
 */
public class SubProcessNode extends BaseFlowNode {

    public static final String NODE_TYPE = "sub_process";
    public static final String DEFAULT_NAME = "子流程";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public SubProcessNode(String id, String name) {
        super(id, name, new ArrayList<>());
    }

    public SubProcessNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME);
    }

    public static SubProcessNode formMap(Map<String, Object> map) {
        return BaseFlowNode.loadFromMap(map, SubProcessNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, SubProcessNode> {
        public Builder() {
            super(new SubProcessNode());
        }
    }
}

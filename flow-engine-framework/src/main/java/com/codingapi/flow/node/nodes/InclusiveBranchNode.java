package com.codingapi.flow.node.nodes;

import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.builder.BaseNodeBuilder;
import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

/**
 * 包容分支节点
 */
public class InclusiveBranchNode extends BaseFlowNode {

    public static final String NODE_TYPE = "inclusive_branch";
    public static final String DEFAULT_NAME = "包容分支节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }


    public InclusiveBranchNode(String id, String name) {
        super(id, name);
    }

    public InclusiveBranchNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME);
    }

    public static InclusiveBranchNode formMap(Map<String, Object> map) {
        return BaseFlowNode.loadFromMap(map, InclusiveBranchNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder,InclusiveBranchNode> {

        public Builder() {
            super(new InclusiveBranchNode());
        }

    }
}

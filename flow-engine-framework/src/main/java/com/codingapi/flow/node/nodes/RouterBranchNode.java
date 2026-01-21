package com.codingapi.flow.node.nodes;

import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

/**
 * 路由分支节点
 */
public class RouterBranchNode extends BaseFlowNode {

    public static final String NODE_TYPE = "router_branch";
    public static final String DEFAULT_NAME = "路由节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }


    public RouterBranchNode(String id, String name) {
        super(id, name);
    }

    public RouterBranchNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME);
    }

    public static RouterBranchNode formMap(Map<String, Object> map) {
        return BaseFlowNode.loadFromMap(map, RouterBranchNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, RouterBranchNode> {
        public Builder() {
            super(new RouterBranchNode());
        }
    }
}

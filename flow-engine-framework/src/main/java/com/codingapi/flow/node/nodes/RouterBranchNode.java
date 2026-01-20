package com.codingapi.flow.node.nodes;

import com.codingapi.flow.node.BaseBranchNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.builder.BranchNodeBuilder;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 路由分支节点
 */
public class RouterBranchNode extends BaseBranchNode {

    public static final String NODE_TYPE = "router_branch";
    public static final String DEFAULT_NAME = "路由节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }


    public RouterBranchNode(String id, String name) {
        super(id, name, new ArrayList<>());
    }

    public RouterBranchNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME);
    }


    public List<IFlowNode> matchRouters(FlowSession session) {
        return null;
    }


    public static RouterBranchNode formMap(Map<String, Object> map) {
        return BaseBranchNode.formMap(map, RouterBranchNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BranchNodeBuilder<Builder, RouterBranchNode> {
        public Builder() {
            super(new RouterBranchNode());
        }
    }
}

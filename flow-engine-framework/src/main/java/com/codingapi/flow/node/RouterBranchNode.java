package com.codingapi.flow.node;

/**
 * 路由分支节点
 */
public class RouterBranchNode extends BaseNode{

    public static final String NODE_TYPE = "router_branch";
    public static final String DEFAULT_NAME = "路由节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public RouterBranchNode() {
        super(DEFAULT_NAME);
    }

    public RouterBranchNode(String name) {
        super(name);
    }
}

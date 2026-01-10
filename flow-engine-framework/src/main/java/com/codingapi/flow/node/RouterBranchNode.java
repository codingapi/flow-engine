package com.codingapi.flow.node;

/**
 * 路由分支节点
 */
public class RouterBranchNode extends BaseNode{

    public static final String NODE_TYPE = "router_branch";

    @Override
    public String getType() {
        return NODE_TYPE;
    }
}

package com.codingapi.flow.node;

/**
 * 包容分支节点
 */
public class InclusiveBranchNode extends BaseNode{

    public static final String NODE_TYPE = "inclusive_branch";

    @Override
    public String getType() {
        return NODE_TYPE;
    }
}

package com.codingapi.flow.node;

/**
 * 包容分支节点
 */
public class InclusiveBranchNode extends BaseNode{

    public static final String NODE_TYPE = "inclusive_branch";
    public static final String DEFAULT_NAME = "包容分支节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public InclusiveBranchNode() {
        super(DEFAULT_NAME);
    }

    public InclusiveBranchNode(String name) {
        super(name);
    }
}

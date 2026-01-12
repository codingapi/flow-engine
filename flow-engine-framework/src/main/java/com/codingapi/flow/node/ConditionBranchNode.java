package com.codingapi.flow.node;

/**
 * 分支节点
 */
public class ConditionBranchNode extends BaseNode{

    public static final String NODE_TYPE = "condition_branch";
    public static final String DEFAULT_NAME = "分支节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public ConditionBranchNode() {
        super(DEFAULT_NAME);
    }

    public ConditionBranchNode(String name) {
        super(name);
    }
}

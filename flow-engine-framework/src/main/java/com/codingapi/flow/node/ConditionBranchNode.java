package com.codingapi.flow.node;

/**
 * 分支节点
 */
public class ConditionBranchNode extends BaseNode{

    public static final String NODE_TYPE = "condition_branch";

    @Override
    public String getType() {
        return NODE_TYPE;
    }
}

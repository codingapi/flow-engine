package com.codingapi.flow.node;

/**
 * 并行节点
 */
public class ParallelBranchNode extends BaseNode{

    public static final String NODE_TYPE = "parallel_branch";

    @Override
    public String getType() {
        return NODE_TYPE;
    }
}

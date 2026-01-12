package com.codingapi.flow.node;

/**
 * 并行节点
 */
public class ParallelBranchNode extends BaseNode{

    public static final String NODE_TYPE = "parallel_branch";
    public static final String DEFAULT_NAME = "并行节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public ParallelBranchNode() {
        super(DEFAULT_NAME);
    }

    public ParallelBranchNode(String name) {
        super(name);
    }
}

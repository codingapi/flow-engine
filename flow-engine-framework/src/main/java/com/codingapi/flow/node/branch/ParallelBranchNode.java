package com.codingapi.flow.node.branch;

import com.codingapi.flow.node.BaseBranchNode;
import com.codingapi.flow.node.builder.BranchNodeBuilder;
import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

/**
 * 并行节点
 */
public class ParallelBranchNode extends BaseBranchNode {

    public static final String NODE_TYPE = "parallel_branch";
    public static final String DEFAULT_NAME = "并行节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public ParallelBranchNode(String id, String name) {
        super(id, name);
    }

    public ParallelBranchNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME);
    }


    public static ParallelBranchNode formMap(Map<String, Object> map) {
        return BaseBranchNode.formMap(map, ParallelBranchNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BranchNodeBuilder<Builder,ParallelBranchNode> {
        public Builder() {
            super(new ParallelBranchNode());
        }
    }
}

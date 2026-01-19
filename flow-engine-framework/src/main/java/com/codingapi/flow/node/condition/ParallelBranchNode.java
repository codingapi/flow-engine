package com.codingapi.flow.node.condition;

import com.codingapi.flow.node.BaseConditionNode;
import com.codingapi.flow.node.builder.ConditionNodeBuilder;
import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

/**
 * 并行节点
 */
public class ParallelBranchNode extends BaseConditionNode {

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
        return BaseConditionNode.formMap(map, ParallelBranchNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ConditionNodeBuilder<Builder,ParallelBranchNode> {
        public Builder() {
            super(new ParallelBranchNode());
        }
    }
}

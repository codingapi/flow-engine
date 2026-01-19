package com.codingapi.flow.node.branch;

import com.codingapi.flow.node.BaseBranchNode;
import com.codingapi.flow.node.builder.BranchNodeBuilder;
import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

/**
 * 包容分支节点
 */
public class InclusiveBranchNode extends BaseBranchNode {

    public static final String NODE_TYPE = "inclusive_branch";
    public static final String DEFAULT_NAME = "包容分支节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }


    public InclusiveBranchNode(String id, String name) {
        super(id, name);
    }

    public InclusiveBranchNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME);
    }

    public static InclusiveBranchNode formMap(Map<String, Object> map) {
        return BaseBranchNode.formMap(map, InclusiveBranchNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BranchNodeBuilder<Builder,InclusiveBranchNode> {

        public Builder() {
            super(new InclusiveBranchNode());
        }

    }
}

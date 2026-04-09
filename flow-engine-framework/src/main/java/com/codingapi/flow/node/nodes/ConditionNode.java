package com.codingapi.flow.node.nodes;

import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.IBlockNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 条件控制节点
 */
public class ConditionNode extends BaseFlowNode implements IBlockNode {

    public static final String NODE_TYPE = NodeType.CONDITION.name();
    public static final String DEFAULT_NAME = "条件控制节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public ConditionNode(String id, String name, int order) {
        super(id, name, order);
    }

    public ConditionNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, 0);
    }

    @Override
    public void addDefaultBranch(int count){
        List<IFlowNode> branches = new ArrayList<>();
        for (int i=0;i<count;i++){
            ConditionBranchNode branchNode = new ConditionBranchNode();
            branchNode.setOrder(i+1);
            branches.add(branchNode);
        }
        branches.add(new ConditionElseBranchNode());
        this.setBlocks(branches);
    }

    public static ConditionNode formMap(Map<String, Object> map) {
        return BaseFlowNode.fromMap(map, ConditionNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, ConditionNode> {

        public Builder() {
            super(new ConditionNode());
        }

    }
}

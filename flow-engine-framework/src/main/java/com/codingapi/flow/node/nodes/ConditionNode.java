package com.codingapi.flow.node.nodes;

import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.IBlockNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.NodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 条件控制节点
 */
public class ConditionNode extends BaseFlowNode implements IBlockNode {

    public static final String NODE_TYPE = NodeType.CONDITION.name();
    public static final String DEFAULT_NAME = "条件节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public static ConditionNode defaultNode() {
        ConditionNode conditionNode = new ConditionNode();
        conditionNode.setId(FlowIDGeneratorGatewayContext.getInstance().generateNodeId());
        conditionNode.setName(DEFAULT_NAME);
        conditionNode.setOrder(0);
        conditionNode.setActions(new ArrayList<>());
        conditionNode.setStrategies(new ArrayList<>());
        return conditionNode;
    }


    @Override
    public void addDefaultBranch(int count) {
        List<IFlowNode> branches = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ConditionBranchNode branchNode = ConditionBranchNode.defaultNode();
            branchNode.setOrder(i + 1);
            branches.add(branchNode);
        }
        branches.add(ConditionElseBranchNode.defaultNode());
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
            super(ConditionNode.defaultNode());
        }

    }
}

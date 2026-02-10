package com.codingapi.flow.node.nodes;

import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.IBlockNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;

import java.util.ArrayList;
import java.util.Comparator;
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

    /**
     * 匹配条件
     */
    @Override
    public boolean handle(FlowSession request) {
        return true;
    }

    @Override
    public List<IFlowNode> filterBranches(List<IFlowNode> nodeList, FlowSession flowSession) {
        List<IFlowNode> nodes = new ArrayList<>();
        for (IFlowNode node : nodeList) {
            if (node.handle(flowSession)) {
                nodes.add(node);
            }
        }
        // 获取最小order的节点
        nodes.sort(Comparator.comparingInt(IFlowNode::getOrder));
        if (!nodes.isEmpty()) {
            return nodes.subList(0, 1);
        }
        return nodes;
    }

    @Override
    public void addDefaultBranch(int count){
        List<IFlowNode> branches = new ArrayList<>();
        for (int i=0;i<count;i++){
            ConditionBranchNode branchNode = new ConditionBranchNode();
            branches.add(branchNode);
        }
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

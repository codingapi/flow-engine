package com.codingapi.flow.node.nodes;

import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 条件else分支节点
 */
public class ConditionElseBranchNode extends BaseFlowNode {

    public static final String NODE_TYPE = NodeType.CONDITION_ELSE_BRANCH.name();
    public static final String DEFAULT_NAME = "条件else分支节点";


    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public ConditionElseBranchNode(String id, String name, int order) {
        super(id, name, order);
    }

    public ConditionElseBranchNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, 100);
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

    public static ConditionElseBranchNode formMap(Map<String, Object> map) {
        return BaseFlowNode.fromMap(map, ConditionElseBranchNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, ConditionElseBranchNode> {

        public Builder() {
            super(new ConditionElseBranchNode());
        }

    }
}

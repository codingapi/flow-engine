package com.codingapi.flow.node.nodes;

import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.script.node.ConditionScript;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 分支节点
 */
public class ConditionBranchNode extends BaseFlowNode {

    public static final String NODE_TYPE = NodeType.CONDITION_BRANCH.name();
    public static final String DEFAULT_NAME = "条件节点节点";

    /**
     * 条件脚本
     */
    @Setter
    private ConditionScript conditionScript;

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public ConditionBranchNode(String id, String name, int order) {
        super(id, name, order);
        this.conditionScript = ConditionScript.defaultScript();
    }

    public ConditionBranchNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, 0);
    }

    /**
     * 匹配条件
     */
    @Override
    public boolean handle(FlowSession request) {
        return conditionScript.execute(request);
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
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("script", conditionScript.getScript());
        return map;
    }

    public static ConditionBranchNode formMap(Map<String, Object> map) {
        ConditionBranchNode branchNode = BaseFlowNode.fromMap(map, ConditionBranchNode.class);
        branchNode.conditionScript = new ConditionScript((String) map.get("script"));
        return branchNode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, ConditionBranchNode> {

        public Builder() {
            super(new ConditionBranchNode());
        }

        public Builder conditionScript(String script) {
            node.conditionScript = new ConditionScript(script);
            return this;
        }
    }
}

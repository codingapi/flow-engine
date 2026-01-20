package com.codingapi.flow.node.nodes;

import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.builder.BaseNodeBuilder;
import com.codingapi.flow.script.node.ConditionScript;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;
import lombok.Setter;

import java.util.Map;

/**
 * 分支节点
 */
public class BranchNodeBranchNode extends BaseFlowNode {

    public static final String NODE_TYPE = "condition_branch";
    public static final String DEFAULT_NAME = "分支节点";

    /**
     * 条件脚本
     */
    @Setter
    private ConditionScript conditionScript;

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public BranchNodeBranchNode(String id, String name, int order) {
        super(id, name, order);
        this.conditionScript = ConditionScript.defaultScript();
    }

    public BranchNodeBranchNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, 0);
    }

    /**
     * 匹配条件
     */
    @Override
    public boolean isContinueTrigger(FlowSession request) {
        return conditionScript.execute(request);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("script", conditionScript.getScript());
        return map;
    }

    public static BranchNodeBranchNode formMap(Map<String, Object> map) {
        BranchNodeBranchNode branchNode = BaseFlowNode.loadFromMap(map, BranchNodeBranchNode.class);
        branchNode.conditionScript = new ConditionScript((String) map.get("script"));
        return branchNode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, BranchNodeBranchNode> {

        public Builder() {
            super(new BranchNodeBranchNode());
        }

        public Builder conditionScript(String script) {
            node.conditionScript = new ConditionScript(script);
            return this;
        }
    }
}

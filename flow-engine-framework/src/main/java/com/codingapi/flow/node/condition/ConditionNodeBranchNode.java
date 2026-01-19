package com.codingapi.flow.node.condition;

import com.codingapi.flow.node.BaseConditionNode;
import com.codingapi.flow.node.builder.ConditionNodeBuilder;
import com.codingapi.flow.script.node.ConditionScript;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;
import lombok.Getter;

import java.util.Map;

/**
 * 分支节点
 */
public class ConditionNodeBranchNode extends BaseConditionNode {

    public static final String NODE_TYPE = "condition_branch";
    public static final String DEFAULT_NAME = "分支节点";

    /**
     * 条件脚本
     */
    private ConditionScript conditionScript;

    /**
     * 条件顺序,越小则优先级越高
     */
    @Getter
    private int order;

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public ConditionNodeBranchNode(String id, String name) {
        super(id, name);
        this.order = 0;
        this.conditionScript = ConditionScript.defaultScript();
    }

    public ConditionNodeBranchNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME);
    }


    /**
     * 匹配条件
     */
    @Override
    public boolean match(FlowSession request) {
        return conditionScript.execute(request);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("script", conditionScript.getScript());
        map.put("order", String.valueOf(order));
        return map;
    }

    public static ConditionNodeBranchNode formMap(Map<String, Object> map) {
        ConditionNodeBranchNode branchNode = BaseConditionNode.formMap(map, ConditionNodeBranchNode.class);
        branchNode.order = Integer.parseInt((String) map.get("order"));
        branchNode.conditionScript = new ConditionScript((String) map.get("script"));
        return branchNode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ConditionNodeBuilder<Builder, ConditionNodeBranchNode> {
        public Builder() {
            super(new ConditionNodeBranchNode());
        }

        public Builder conditionScript(String script) {
            node.conditionScript = new ConditionScript(script);
            return this;
        }

        public Builder order(int order) {
            node.order = order;
            return this;
        }
    }
}

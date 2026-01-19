package com.codingapi.flow.node.branch;

import com.codingapi.flow.node.BaseBranchNode;
import com.codingapi.flow.node.builder.BranchNodeBuilder;
import com.codingapi.flow.script.node.ConditionScript;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

/**
 * 分支节点
 */
public class BranchNodeBranchNode extends BaseBranchNode {

    public static final String NODE_TYPE = "condition_branch";
    public static final String DEFAULT_NAME = "分支节点";

    /**
     * 条件脚本
     */
    private ConditionScript conditionScript;

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public BranchNodeBranchNode(String id, String name) {
        super(id, name);
        this.conditionScript = ConditionScript.defaultScript();
    }

    public BranchNodeBranchNode() {
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
        return map;
    }

    public static BranchNodeBranchNode formMap(Map<String, Object> map) {
        BranchNodeBranchNode branchNode = BaseBranchNode.formMap(map, BranchNodeBranchNode.class);
        branchNode.conditionScript = new ConditionScript((String) map.get("script"));
        return branchNode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BranchNodeBuilder<Builder, BranchNodeBranchNode> {
        public Builder() {
            super(new BranchNodeBranchNode());
        }

        public Builder conditionScript(String script) {
            node.conditionScript = new ConditionScript(script);
            return this;
        }



    }
}

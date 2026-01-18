package com.codingapi.flow.node;

import com.codingapi.flow.action.DefaultAction;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.PassAction;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.script.node.ConditionScript;
import com.codingapi.flow.script.node.ErrorTriggerScript;
import com.codingapi.flow.script.node.NodeTitleScript;
import com.codingapi.flow.script.node.OperatorLoadScript;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.*;
import com.codingapi.flow.utils.RandomUtils;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 分支节点
 */
public class ConditionNodeBranchNode extends BaseNode implements IConditionNode {

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


    public ConditionNodeBranchNode(String id, String name, String view, OperatorLoadScript operatorScript, NodeTitleScript nodeTitleScript, ErrorTriggerScript errorTriggerScript, List<FormFieldPermission> formFieldsPermissions, List<IFlowAction> actions, List<INodeStrategy> nodeStrategies) {
        super(id, name, view, operatorScript, nodeTitleScript, errorTriggerScript, formFieldsPermissions, actions, nodeStrategies);
        this.order = 0;
        this.conditionScript = ConditionScript.defaultScript();
    }

    protected ConditionNodeBranchNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, DEFAULT_VIEW, OperatorLoadScript.creator(), NodeTitleScript.defaultScript(), ErrorTriggerScript.defaultNodeScript(), new ArrayList<>(), defaultActions(), defaultStrategies());
    }


    /**
     * 匹配条件
     */
    @Override
    public boolean match(FlowSession request) {
        return conditionScript.execute(request);
    }

    private static List<INodeStrategy> defaultStrategies() {
        List<INodeStrategy> strategies = new ArrayList<>();
        strategies.add(TimeoutStrategy.defaultStrategy());
        strategies.add(MultiOperatorAuditStrategy.defaultStrategy());
        strategies.add(SameOperatorAuditStrategy.defaultStrategy());
        strategies.add(RecordMergeStrategy.defaultStrategy());
        strategies.add(ResubmitStrategy.defaultStrategy());
        strategies.add(AdviceStrategy.defaultStrategy());
        return strategies;
    }

    private static List<IFlowAction> defaultActions() {
        List<IFlowAction> actions = new ArrayList<>();
        actions.add(new DefaultAction());
        return actions;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("script", conditionScript.getScript());
        map.put("order", String.valueOf(order));
        return map;
    }

    public static ConditionNodeBranchNode formMap(Map<String, Object> map) {
        ConditionNodeBranchNode branchNode = BaseNode.formMap(map, ConditionNodeBranchNode.class);
        branchNode.order = Integer.parseInt((String) map.get("order"));
        branchNode.conditionScript = new ConditionScript((String) map.get("script"));
        return branchNode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseBuilder<Builder, ConditionNodeBranchNode> {
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

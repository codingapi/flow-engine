package com.codingapi.flow.node;

import com.codingapi.flow.action.DefaultAction;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.script.node.ErrorTriggerScript;
import com.codingapi.flow.script.node.NodeTitleScript;
import com.codingapi.flow.script.node.OperatorLoadScript;
import com.codingapi.flow.strategy.*;
import com.codingapi.flow.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 路由分支节点
 */
public class RouterBranchNode extends BaseNode {

    public static final String NODE_TYPE = "router_branch";
    public static final String DEFAULT_NAME = "路由节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }


    public RouterBranchNode(String id, String name, String view, OperatorLoadScript operatorScript, NodeTitleScript nodeTitleScript, ErrorTriggerScript errorTriggerScript, List<FormFieldPermission> formFieldsPermissions, List<IFlowAction> actions, List<INodeStrategy> nodeStrategies) {
        super(id, name, view, operatorScript, nodeTitleScript, errorTriggerScript, formFieldsPermissions, actions, nodeStrategies);
    }

    protected RouterBranchNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, DEFAULT_VIEW, OperatorLoadScript.creator(), NodeTitleScript.defaultScript(), ErrorTriggerScript.defaultNodeScript(), new ArrayList<>(), defaultActions(), defaultStrategies());
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

    public static RouterBranchNode formMap(Map<String, Object> map) {
        return BaseNode.formMap(map, RouterBranchNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseBuilder<Builder,RouterBranchNode> {
        public Builder() {
            super(new RouterBranchNode());
        }
    }
}

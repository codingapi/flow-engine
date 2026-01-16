package com.codingapi.flow.node;

import com.codingapi.flow.action.FlowAction;
import com.codingapi.flow.action.factory.FlowActionFactory;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.script.ErrorTriggerScript;
import com.codingapi.flow.script.NodeTitleScript;
import com.codingapi.flow.script.OperatorLoadScript;
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


    public RouterBranchNode(String id, String name, String view, OperatorLoadScript operatorScript, NodeTitleScript nodeTitleScript, ErrorTriggerScript errorTriggerScript, List<FormFieldPermission> formFieldsPermissions, List<FlowAction> actions) {
        super(id, name, view, operatorScript, nodeTitleScript, errorTriggerScript, formFieldsPermissions,actions);
    }

    protected RouterBranchNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, DEFAULT_VIEW, OperatorLoadScript.creator(), NodeTitleScript.defaultScript(), ErrorTriggerScript.defaultNodeScript(), new ArrayList<>(),defaultActions());
    }

    private static List<FlowAction> defaultActions() {
        List<FlowAction> actions = new ArrayList<>();
        actions.add(FlowActionFactory.getInstance().defaultAction());
        return actions;
    }

    public static RouterBranchNode formMap(Map<String, Object> map) {
        return BaseNode.formMap(map, RouterBranchNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseBuilder<RouterBranchNode> {
        public Builder() {
            super(new RouterBranchNode());
        }
    }
}

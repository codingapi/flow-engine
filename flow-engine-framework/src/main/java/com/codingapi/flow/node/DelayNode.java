package com.codingapi.flow.node;

import com.codingapi.flow.action.ActionType;
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
 * 延迟节点
 */
public class DelayNode extends BaseNode {

    public static final String NODE_TYPE = "delay";
    public static final String DEFAULT_NAME = "延迟节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public DelayNode(String id, String name, String view, OperatorLoadScript operatorScript, NodeTitleScript nodeTitleScript, ErrorTriggerScript errorTriggerScript, List<FormFieldPermission> formFieldsPermissions, List<FlowAction> actions) {
        super(id, name, view, operatorScript, nodeTitleScript, errorTriggerScript, formFieldsPermissions,actions);
    }

    protected DelayNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, DEFAULT_VIEW, OperatorLoadScript.creator(), NodeTitleScript.defaultScript(), ErrorTriggerScript.defaultNodeScript(), new ArrayList<>(),defaultActions());
    }

    private static List<FlowAction> defaultActions() {
        List<FlowAction> actions = new ArrayList<>();
        actions.add(FlowActionFactory.getInstance().create(ActionType.PASS));
        actions.add(FlowActionFactory.getInstance().create(ActionType.REJECT));
        actions.add(FlowActionFactory.getInstance().create(ActionType.RETURN));
        actions.add(FlowActionFactory.getInstance().create(ActionType.TRANSFER));
        actions.add(FlowActionFactory.getInstance().create(ActionType.CANCEL));
        return actions;
    }

    public static DelayNode formMap(Map<String, Object> map) {
        return BaseNode.formMap(map, DelayNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseBuilder<DelayNode> {
        public Builder() {
            super(new DelayNode());
        }
    }
}

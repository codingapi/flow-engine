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
 * 开始节点
 */
public class StartNode extends BaseNode {

    public static final String NODE_TYPE = "start";
    public static final String DEFAULT_NAME = "开始节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public StartNode(String id, String name, String view, OperatorLoadScript operatorScript, NodeTitleScript nodeTitleScript, ErrorTriggerScript errorTriggerScript, List<FormFieldPermission> formFieldsPermissions, List<FlowAction> actions,long timeoutTime, boolean mergeable) {
        super(id, name, view, operatorScript, nodeTitleScript, errorTriggerScript, formFieldsPermissions,actions,timeoutTime, mergeable);
    }

    protected StartNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, DEFAULT_VIEW, OperatorLoadScript.creator(), NodeTitleScript.defaultScript(), ErrorTriggerScript.defaultNodeScript(), new ArrayList<>(),defaultActions(),0, false);
    }

    private static List<FlowAction> defaultActions() {
        List<FlowAction> actions = new ArrayList<>();
        actions.add(FlowActionFactory.getInstance().create(ActionType.PASS));
        return actions;
    }


    public static StartNode formMap(Map<String, Object> map) {
        return BaseNode.formMap(map, StartNode.class);
    }
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseBuilder<StartNode> {

        public Builder() {
            super(new StartNode());
        }
    }
}

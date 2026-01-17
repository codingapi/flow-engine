package com.codingapi.flow.node;

import com.codingapi.flow.action.DefaultAction;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.script.ErrorTriggerScript;
import com.codingapi.flow.script.NodeTitleScript;
import com.codingapi.flow.script.OperatorLoadScript;
import com.codingapi.flow.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 包容分支节点
 */
public class InclusiveBranchNode extends BaseNode{

    public static final String NODE_TYPE = "inclusive_branch";
    public static final String DEFAULT_NAME = "包容分支节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public InclusiveBranchNode(String id, String name, String view, OperatorLoadScript operatorScript, NodeTitleScript nodeTitleScript, ErrorTriggerScript errorTriggerScript, List<FormFieldPermission> formFieldsPermissions, List<IFlowAction> actions,long timeoutTime, boolean mergeable) {
        super(id, name, view, operatorScript, nodeTitleScript, errorTriggerScript, formFieldsPermissions,actions,timeoutTime, mergeable);
    }

    protected InclusiveBranchNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, DEFAULT_VIEW, OperatorLoadScript.creator(), NodeTitleScript.defaultScript(), ErrorTriggerScript.defaultNodeScript(), new ArrayList<>(),defaultActions(),0, false);
    }

    private static List<IFlowAction> defaultActions() {
        List<IFlowAction> actions = new ArrayList<>();
        actions.add(new DefaultAction());
        return actions;
    }

    public static InclusiveBranchNode formMap(Map<String, Object> map) {
        return BaseNode.formMap(map, InclusiveBranchNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseBuilder<InclusiveBranchNode>{

        public Builder() {
            super(new InclusiveBranchNode());
        }

    }
}

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
 * 并行节点
 */
public class ParallelBranchNode extends BaseNode{

    public static final String NODE_TYPE = "parallel_branch";
    public static final String DEFAULT_NAME = "并行节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public ParallelBranchNode(String id, String name, String view, OperatorLoadScript operatorScript, NodeTitleScript nodeTitleScript, ErrorTriggerScript errorTriggerScript, List<FormFieldPermission> formFieldsPermissions, List<IFlowAction> actions, long timeoutTime, boolean mergeable) {
        super(id, name, view, operatorScript, nodeTitleScript, errorTriggerScript, formFieldsPermissions,actions,timeoutTime, mergeable);
    }

    protected ParallelBranchNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, DEFAULT_VIEW, OperatorLoadScript.creator(), NodeTitleScript.defaultScript(), ErrorTriggerScript.defaultNodeScript(), new ArrayList<>(),defaultActions(),0, false);
    }

    private static List<IFlowAction> defaultActions() {
        List<IFlowAction> actions = new ArrayList<>();
        actions.add(new DefaultAction());
        return actions;
    }

    public static ParallelBranchNode formMap(Map<String, Object> map) {
        return BaseNode.formMap(map, ParallelBranchNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseBuilder<ParallelBranchNode> {
        public Builder() {
            super(new ParallelBranchNode());
        }
    }
}

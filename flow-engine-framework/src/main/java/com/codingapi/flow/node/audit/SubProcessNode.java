package com.codingapi.flow.node.audit;

import com.codingapi.flow.action.DefaultAction;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.node.builder.AuditNodeBuilder;
import com.codingapi.flow.node.BaseAuditNode;
import com.codingapi.flow.script.node.ErrorTriggerScript;
import com.codingapi.flow.script.node.NodeTitleScript;
import com.codingapi.flow.script.node.OperatorLoadScript;
import com.codingapi.flow.strategy.*;
import com.codingapi.flow.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 子流程
 */
public class SubProcessNode extends BaseAuditNode {

    public static final String NODE_TYPE = "sub_process";
    public static final String DEFAULT_NAME = "子流程";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public SubProcessNode(String id, String name, String view, OperatorLoadScript operatorScript, NodeTitleScript nodeTitleScript, ErrorTriggerScript errorTriggerScript, List<FormFieldPermission> formFieldsPermissions, List<IFlowAction> actions, List<INodeStrategy> nodeStrategies) {
        super(id, name, view, operatorScript, nodeTitleScript, errorTriggerScript, formFieldsPermissions, actions, nodeStrategies);
    }

    public SubProcessNode() {
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

    public static SubProcessNode formMap(Map<String, Object> map) {
        return BaseAuditNode.formMap(map, SubProcessNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AuditNodeBuilder<Builder,SubProcessNode> {
        public Builder() {
            super(new SubProcessNode());
        }
    }
}

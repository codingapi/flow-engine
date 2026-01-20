package com.codingapi.flow.node.nodes;

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
 * 结束节点
 */
public class EndNode extends BaseAuditNode {

    public static final String NODE_TYPE = "end";
    public static final String DEFAULT_NAME = "结束节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }


    public EndNode(String id, String name, List<IFlowAction> actions, String view, OperatorLoadScript operatorScript, NodeTitleScript nodeTitleScript, ErrorTriggerScript errorTriggerScript, List<FormFieldPermission> formFieldsPermissions, List<INodeStrategy> nodeStrategies) {
        super(id, name,actions, view, operatorScript, nodeTitleScript, errorTriggerScript, formFieldsPermissions,  nodeStrategies);
    }

    public EndNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, defaultActions(),  DEFAULT_VIEW, OperatorLoadScript.creator(), NodeTitleScript.defaultScript(), ErrorTriggerScript.defaultNodeScript(), new ArrayList<>(),defaultStrategies());
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

    public static EndNode formMap(Map<String, Object> map) {
        return BaseAuditNode.formMap(map, EndNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AuditNodeBuilder<Builder,EndNode> {

        public Builder() {
            super(new EndNode());
        }
    }
}

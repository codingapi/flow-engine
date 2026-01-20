package com.codingapi.flow.node.nodes;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.PassAction;
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
 * 办理节点
 */
public class HandleNode extends BaseAuditNode {

    public static final String NODE_TYPE = "handle";
    public static final String DEFAULT_NAME = "办理节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }


    public HandleNode(String id, String name,List<IFlowAction> actions, String view, OperatorLoadScript operatorScript, NodeTitleScript nodeTitleScript, ErrorTriggerScript errorTriggerScript, List<FormFieldPermission> formFieldsPermissions,  List<INodeStrategy> nodeStrategies) {
        super(id, name,actions, view, operatorScript, nodeTitleScript, errorTriggerScript, formFieldsPermissions, nodeStrategies);
    }

    public HandleNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, defaultActions(), DEFAULT_VIEW, OperatorLoadScript.creator(), NodeTitleScript.defaultScript(), ErrorTriggerScript.defaultNodeScript(),new ArrayList<>(), defaultStrategies());
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
        actions.add(new PassAction());
        return actions;
    }

    public static HandleNode formMap(Map<String, Object> map) {
        return BaseAuditNode.formMap(map, HandleNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AuditNodeBuilder<Builder,HandleNode> {
        public Builder() {
            super(new HandleNode());
        }
    }
}

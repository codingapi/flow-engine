package com.codingapi.flow.node.nodes;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.AddAuditAction;
import com.codingapi.flow.action.actions.PassAction;
import com.codingapi.flow.action.actions.RejectAction;
import com.codingapi.flow.action.actions.SaveAction;
import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.node.BaseAuditNode;
import com.codingapi.flow.strategy.*;
import com.codingapi.flow.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 审批节点
 */
public class ApprovalNode extends BaseAuditNode {

    public static final String NODE_TYPE = "approval";
    public static final String DEFAULT_NAME = "审批节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public ApprovalNode(String id, String name, String view, List<IFlowAction> actions, List<INodeStrategy> nodeStrategies) {
        super(id, name, view, actions, nodeStrategies);
    }

    public ApprovalNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, DEFAULT_VIEW, defaultActions(), defaultStrategies());
    }

    private static List<INodeStrategy> defaultStrategies() {
        List<INodeStrategy> strategies = new ArrayList<>();
        strategies.add(TimeoutStrategy.defaultStrategy());
        strategies.add(MultiOperatorAuditStrategy.defaultStrategy());
        strategies.add(SameOperatorAuditStrategy.defaultStrategy());
        strategies.add(RecordMergeStrategy.defaultStrategy());
        strategies.add(ResubmitStrategy.defaultStrategy());
        strategies.add(AdviceStrategy.defaultStrategy());
        strategies.add(ErrorTriggerStrategy.defaultStrategy());
        strategies.add(NodeTitleStrategy.defaultStrategy());
        strategies.add(FormFieldPermissionStrategy.defaultStrategy());
        strategies.add(OperatorLoadStrategy.defaultStrategy());
        return strategies;
    }

    private static List<IFlowAction> defaultActions() {
        List<IFlowAction> actions = new ArrayList<>();
        actions.add(new PassAction());
        actions.add(new RejectAction());
        actions.add(new SaveAction());
        actions.add(new AddAuditAction());
        return actions;
    }


    public static ApprovalNode formMap(Map<String, Object> map) {
        return BaseAuditNode.formMap(map, ApprovalNode.class);
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, ApprovalNode> {

        public Builder() {
            super(new ApprovalNode());
        }
    }
}

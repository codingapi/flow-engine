package com.codingapi.flow.node.nodes;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.*;
import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.node.BaseAuditNode;
import com.codingapi.flow.node.IDisplayNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.strategy.node.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 审批节点
 */
public class ApprovalNode extends BaseAuditNode implements IDisplayNode {

    public static final String NODE_TYPE = NodeType.APPROVAL.name();
    public static final String DEFAULT_NAME = "审批节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }


    public static ApprovalNode defaultNode() {
        ApprovalNode approvalNode = new ApprovalNode();
        approvalNode.setId(FlowIDGeneratorGatewayContext.getInstance().generateNodeId());
        approvalNode.setName(DEFAULT_NAME);
        approvalNode.setView(DEFAULT_VIEW);
        approvalNode.setCode(FlowIDGeneratorGatewayContext.getInstance().generateViewCode());
        approvalNode.setActions(defaultActions());
        approvalNode.setStrategies(defaultStrategies());
        return approvalNode;
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
        strategies.add(RevokeStrategy.defaultStrategy());
        return strategies;
    }

    private static List<IFlowAction> defaultActions() {
        List<IFlowAction> actions = new ArrayList<>();
        actions.add(PassAction.defaultAction());
        actions.add(RejectAction.defaultAction());
        actions.add(SaveAction.defaultAction());
        actions.add(AddAuditAction.defaultAction());
        actions.add(TransferAction.defaultAction());
        actions.add(ReturnAction.defaultAction());
        actions.add(DelegateAction.defaultAction());
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
            super(ApprovalNode.defaultNode());
        }
    }
}

package com.codingapi.flow.node.nodes;

import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.manager.NodeStrategyManager;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.IDisplayNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.node.INodeStrategy;
import com.codingapi.flow.strategy.node.SubProcessStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 子流程
 */
public class SubProcessNode extends BaseFlowNode implements IDisplayNode {

    public static final String NODE_TYPE = NodeType.SUB_PROCESS.name();
    public static final String DEFAULT_NAME = "子流程";

    @Override
    public String getType() {
        return NODE_TYPE;
    }


    public static SubProcessNode defaultNode(){
        SubProcessNode subProcessNode = new SubProcessNode();
        subProcessNode.setId(FlowIDGeneratorGatewayContext.getInstance().generateNodeId());
        subProcessNode.setName(DEFAULT_NAME);
        subProcessNode.setOrder(0);
        subProcessNode.setStrategies(defaultStrategies());
        subProcessNode.setActions(new ArrayList<>());
        return subProcessNode;
    }

    private static List<INodeStrategy> defaultStrategies() {
        List<INodeStrategy> strategies = new ArrayList<>();
        strategies.add(SubProcessStrategy.defaultStrategy());
        return strategies;
    }


    @Override
    public boolean handle(FlowSession session) {
        if(super.handle(session)) {
            NodeStrategyManager nodeStrategyManager = this.strategyManager();
            SubProcessStrategy processStrategy = nodeStrategyManager.getStrategy(SubProcessStrategy.class);
            processStrategy.execute(session);
            return true;
        }
        return false;
    }

    public static SubProcessNode formMap(Map<String, Object> map) {
        return BaseFlowNode.fromMap(map, SubProcessNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, SubProcessNode> {
        public Builder() {
            super(SubProcessNode.defaultNode());
        }
    }
}

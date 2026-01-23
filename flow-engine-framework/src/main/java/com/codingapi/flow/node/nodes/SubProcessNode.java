package com.codingapi.flow.node.nodes;

import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.node.manager.StrategyManager;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.INodeStrategy;
import com.codingapi.flow.strategy.SubProcessStrategy;
import com.codingapi.flow.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 子流程
 */
public class SubProcessNode extends BaseFlowNode {

    public static final String NODE_TYPE = NodeType.SUB_PROCESS.name();
    public static final String DEFAULT_NAME = "子流程";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public SubProcessNode(String id, String name) {
        super(id, name, 0, new ArrayList<>(), defaultStrategies());
    }

    public SubProcessNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME);
    }

    private static List<INodeStrategy> defaultStrategies() {
        List<INodeStrategy> strategies = new ArrayList<>();
        strategies.add(SubProcessStrategy.defaultStrategy());
        return strategies;
    }


    @Override
    public boolean handle(FlowSession session) {
        StrategyManager strategyManager = this.strategyManager();
        SubProcessStrategy processStrategy = strategyManager.getStrategy(SubProcessStrategy.class);
        processStrategy.execute(session);
        return true;
    }

    public static SubProcessNode formMap(Map<String, Object> map) {
        return BaseFlowNode.loadFromMap(map, SubProcessNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, SubProcessNode> {
        public Builder() {
            super(new SubProcessNode());
        }
    }
}

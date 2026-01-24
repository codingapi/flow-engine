package com.codingapi.flow.node.nodes;

import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.manager.NodeStrategyManager;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.node.INodeStrategy;
import com.codingapi.flow.strategy.node.TriggerStrategy;
import com.codingapi.flow.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 触发节点
 */
public class TriggerNode extends BaseFlowNode {

    public static final String NODE_TYPE = NodeType.TRIGGER.name();
    public static final String DEFAULT_NAME = "触发节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public TriggerNode(String id, String name) {
        super(id, name, 0, new ArrayList<>(), defaultStrategies());
    }

    public TriggerNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME);
    }

    private static List<INodeStrategy> defaultStrategies() {
        List<INodeStrategy> strategies = new ArrayList<>();
        strategies.add(TriggerStrategy.defaultStrategy());
        return strategies;
    }


    @Override
    public boolean handle(FlowSession session) {
        NodeStrategyManager nodeStrategyManager = this.strategyManager();
        TriggerStrategy triggerStrategy = nodeStrategyManager.getStrategy(TriggerStrategy.class);
        triggerStrategy.execute(session);
        return true;
    }

    public static TriggerNode formMap(Map<String, Object> map) {
        return BaseFlowNode.fromMap(map, TriggerNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, TriggerNode> {
        public Builder() {
            super(new TriggerNode());
        }
    }
}

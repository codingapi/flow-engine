package com.codingapi.flow.node.nodes;

import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.domain.DelayTask;
import com.codingapi.flow.domain.DelayTaskManager;
import com.codingapi.flow.manager.NodeStrategyManager;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.node.DelayStrategy;
import com.codingapi.flow.strategy.node.INodeStrategy;
import com.codingapi.flow.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 延迟节点
 */
public class DelayNode extends BaseFlowNode {

    public static final String NODE_TYPE = NodeType.DELAY.name();
    public static final String DEFAULT_NAME = "延迟节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    @Override
    public boolean handle(FlowSession session) {
        if(super.handle(session)) {
            NodeStrategyManager nodeStrategyManager = this.strategyManager();
            DelayStrategy delayStrategy = nodeStrategyManager.getStrategy(DelayStrategy.class);
            if (delayStrategy != null) {
                FlowRecord currentRecord = session.getCurrentRecord();
                DelayTask delayTask = new DelayTask(delayStrategy, currentRecord, this.getId());
                DelayTaskManager.getInstance().addTask(delayTask);
            }
            return false;
        }
        return false;
    }


    public DelayNode(String id, String name) {
        super(id, name, 0, new ArrayList<>(), defaultStrategies());
    }

    public DelayNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME);
    }

    private static List<INodeStrategy> defaultStrategies() {
        List<INodeStrategy> strategies = new ArrayList<>();
        strategies.add(DelayStrategy.defaultStrategy());
        return strategies;
    }

    public static DelayNode formMap(Map<String, Object> map) {
        return BaseFlowNode.fromMap(map, DelayNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, DelayNode> {
        public Builder() {
            super(new DelayNode());
        }
    }
}

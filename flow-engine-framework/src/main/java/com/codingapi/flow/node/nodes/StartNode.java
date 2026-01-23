package com.codingapi.flow.node.nodes;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.PassAction;
import com.codingapi.flow.action.actions.SaveAction;
import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.manager.StrategyManager;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.INodeStrategy;
import com.codingapi.flow.strategy.NodeTitleStrategy;
import com.codingapi.flow.utils.RandomUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 开始节点
 */
public class StartNode extends BaseFlowNode {

    public static final String NODE_TYPE = "start";
    public static final String DEFAULT_NAME = "开始节点";

    public static final String DEFAULT_VIEW = "default";

    /**
     * 渲染视图
     */
    @Getter
    @Setter
    private String view;


    @Override
    public String getType() {
        return NODE_TYPE;
    }


    public StartNode(String id, String name, String view, List<IFlowAction> actions, List<INodeStrategy> nodeStrategies) {
        super(id, name, 0, actions, nodeStrategies);
        this.view = view;
    }

    public StartNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, DEFAULT_VIEW, defaultActions(), defaultStrategies());
    }


    @Override
    public List<FlowRecord> generateCurrentRecords(FlowSession session) {
        List<FlowRecord> records = new ArrayList<>();
        FlowRecord currentRecord = session.getCurrentRecord();
        IFlowOperator operator = session.getCurrentOperator();
        IFlowAction action = session.getCurrentAction();
        if (currentRecord == null) {
            FlowRecord flowRecord = new FlowRecord(session.updateSession(operator), action.id(), 0);
            records.add(flowRecord);
        } else {
            // 获取流程创建者
            IFlowOperator creatorOperator = GatewayContext.getInstance().getFlowOperator(currentRecord.getCreateOperatorId());
            FlowRecord flowRecord = new FlowRecord(session.updateSession(creatorOperator), action.id(), 0);
            records.add(flowRecord);
        }
        return records;
    }


    private static List<INodeStrategy> defaultStrategies() {
        List<INodeStrategy> strategies = new ArrayList<>();
        strategies.add(NodeTitleStrategy.defaultStrategy());
        strategies.add(FormFieldPermissionStrategy.defaultStrategy());
        return strategies;
    }

    private static List<IFlowAction> defaultActions() {
        List<IFlowAction> actions = new ArrayList<>();
        actions.add(new PassAction());
        actions.add(new SaveAction());
        return actions;
    }

    public static StartNode formMap(Map<String, Object> map) {
        StartNode startNode = BaseFlowNode.loadFromMap(map, StartNode.class);
        startNode.setView((String) map.get("view"));
        return startNode;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("view", view);
        return map;
    }

    @Override
    public void fillNewRecord(FlowSession session, FlowRecord flowRecord) {
        StrategyManager strategyManager = this.strategyManager();
        flowRecord.setTitle(strategyManager.generateTitle(session));
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, StartNode> {
        public Builder() {
            super(new StartNode());
        }
    }
}

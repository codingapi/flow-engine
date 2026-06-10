package com.codingapi.flow.node.nodes;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.PassAction;
import com.codingapi.flow.action.actions.SaveAction;
import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.javscript.annotation.NodeViewScript;
import com.codingapi.flow.manager.NodeStrategyManager;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.IDisplayNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.INodeStrategy;
import com.codingapi.flow.strategy.node.NodeTitleStrategy;
import com.codingapi.flow.strategy.node.RevokeStrategy;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 开始节点
 */
public class StartNode extends BaseFlowNode implements IDisplayNode {

    public static final String NODE_TYPE = NodeType.START.name();
    public static final String DEFAULT_NAME = "开始节点";

    public static final String DEFAULT_VIEW = "default";

    /**
     * 渲染视图
     */
    @Getter
    @Setter
    private String view;

    /**
     * 视图代码
     */
    @Getter
    @Setter
    @NodeViewScript
    private String code;


    @Override
    public String getType() {
        return NODE_TYPE;
    }


    public static StartNode defaultNode(){
        StartNode startNode = new StartNode();
        startNode.setId(FlowIDGeneratorGatewayContext.getInstance().generateNodeId());
        startNode.setName(DEFAULT_NAME);
        startNode.setView(DEFAULT_VIEW);
        startNode.setCode(FlowIDGeneratorGatewayContext.getInstance().generateViewCode());
        startNode.setActions(defaultActions());
        startNode.setStrategies(defaultStrategies());
        startNode.setOrder(0);
        return startNode;
    }


    @Override
    public List<FlowRecord> generateCurrentRecords(FlowSession session) {
        List<FlowRecord> records = new ArrayList<>();
        FlowRecord currentRecord = session.getCurrentRecord();
        IFlowOperator operator = session.getCurrentOperator();
        if (currentRecord == null) {
            FlowRecord flowRecord = new FlowRecord(session.updateSession(operator), 0);
            session.setCurrentRecord(flowRecord);
            records.add(flowRecord);
        } else {
            // 获取流程创建者
            IFlowOperator creatorOperator = GatewayContext.getInstance().getFlowOperator(currentRecord.getCreateOperatorId());
            FlowRecord flowRecord = new FlowRecord(session.updateSession(creatorOperator), 0);
            records.add(flowRecord);
        }
        return records;
    }


    private static List<INodeStrategy> defaultStrategies() {
        List<INodeStrategy> strategies = new ArrayList<>();
        strategies.add(NodeTitleStrategy.defaultStrategy());
        strategies.add(FormFieldPermissionStrategy.defaultStrategy());
        strategies.add(RevokeStrategy.defaultStrategy());
        return strategies;
    }

    private static List<IFlowAction> defaultActions() {
        List<IFlowAction> actions = new ArrayList<>();
        actions.add(PassAction.defaultAction());
        actions.add(SaveAction.defaultAction());
        return actions;
    }

    public static StartNode formMap(Map<String, Object> map) {
        StartNode startNode = BaseFlowNode.fromMap(map, StartNode.class);
        startNode.setView((String) map.get("view"));
        startNode.setCode((String) map.get("code"));
        return startNode;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("view", view);
        map.put("code", code);
        return map;
    }

    @Override
    public void fillNewRecord(FlowSession session, FlowRecord flowRecord) {
        NodeStrategyManager nodeStrategyManager = this.strategyManager();
        flowRecord.setTitle(nodeStrategyManager.generateTitle(session));
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, StartNode> {
        public Builder() {
            super(StartNode.defaultNode());
        }
    }
}

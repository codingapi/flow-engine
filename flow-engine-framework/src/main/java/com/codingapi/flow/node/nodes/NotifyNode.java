package com.codingapi.flow.node.nodes;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.node.BaseAuditNode;
import com.codingapi.flow.node.manager.OperatorManager;
import com.codingapi.flow.node.manager.StrategyManager;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.*;
import com.codingapi.flow.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 抄送节点
 */
public class NotifyNode extends BaseAuditNode {

    public static final String NODE_TYPE = "notify";
    public static final String DEFAULT_NAME = "抄送节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public NotifyNode(String id, String name, String view, List<IFlowAction> actions, List<INodeStrategy> nodeStrategies) {
        super(id, name, view, actions, nodeStrategies);
    }

    public NotifyNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, DEFAULT_VIEW, defaultActions(), defaultStrategies());
    }

    @Override
    public boolean handle(FlowSession session) {
        List<FlowRecord> records = this.generateCurrentRecords(session);
        for (FlowRecord record : records) {
            this.fillNewRecord(session, record);
        }
        RepositoryHolderContext.getInstance().saveRecords(records);
        return true;
    }

    @Override
    public void fillNewRecord(FlowSession session, FlowRecord flowRecord) {
        StrategyManager strategyManager = this.strategyManager();
        flowRecord.setTitle(strategyManager.generateTitle(session));
        flowRecord.setTimeoutTime(strategyManager.getTimeoutTime());
        flowRecord.setMergeable(strategyManager.isMergeable());
        flowRecord.finish(true);
    }

    @Override
    public boolean isDone(FlowSession session) {
        return true;
    }

    /**
     * 生成当前节点的记录
     *
     * @param session 触发会话
     * @return 生成当前节点的记录
     */
    @Override
    public List<FlowRecord> generateCurrentRecords(FlowSession session) {
        List<FlowRecord> records = new ArrayList<>();
        StrategyManager strategyManager = this.strategyManager();
        OperatorManager operatorManager = strategyManager.loadOperators(session);
        List<IFlowOperator> operators = operatorManager.getOperators();
        for (int order = 0; order < operators.size(); order++) {
            IFlowOperator operator = operators.get(order);
            FlowRecord flowRecord = new FlowRecord(session.updateSession(operator), order);
            records.add(flowRecord);
        }
        return records;
    }

    private static List<INodeStrategy> defaultStrategies() {
        List<INodeStrategy> strategies = new ArrayList<>();
        strategies.add(ErrorTriggerStrategy.defaultStrategy());
        strategies.add(NodeTitleStrategy.defaultStrategy());
        strategies.add(FormFieldPermissionStrategy.defaultStrategy());
        strategies.add(OperatorLoadStrategy.defaultStrategy());
        return strategies;
    }

    private static List<IFlowAction> defaultActions() {
        return new ArrayList<>();
    }

    public static NotifyNode formMap(Map<String, Object> map) {
        return BaseAuditNode.formMap(map, NotifyNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, NotifyNode> {
        public Builder() {
            super(new NotifyNode());
        }
    }
}

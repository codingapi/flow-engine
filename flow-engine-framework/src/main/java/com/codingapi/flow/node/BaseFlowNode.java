package com.codingapi.flow.node;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.ReturnAction;
import com.codingapi.flow.action.SaveAction;
import com.codingapi.flow.action.TransferAction;
import com.codingapi.flow.context.RepositoryContext;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.node.builder.NodeMapBuilder;
import com.codingapi.flow.node.manager.ActionManager;
import com.codingapi.flow.node.manager.StrategyManager;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowAdvice;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.INodeStrategy;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseFlowNode implements IFlowNode {

    /**
     * 节点id
     */
    @Getter
    @Setter
    protected String id;
    /**
     * 节点名称
     */
    @Getter
    @Setter
    protected String name;

    /**
     * 条件顺序,越小则优先级越高
     */
    @Getter
    @Setter
    protected int order;

    /**
     * 节点操作
     */
    @Setter
    @Getter
    protected List<IFlowAction> actions;

    /**
     * 节点策略
     */
    @Getter
    protected List<INodeStrategy> strategies;


    public void setStrategies(List<INodeStrategy> strategies) {
        if(strategies!=null && !strategies.isEmpty()) {
            if(this.strategies!=null){
                this.strategies.addAll(strategies);
            }else {
                this.strategies = strategies;
            }
        }
    }

    public BaseFlowNode(String name, String id) {
        this(name, id, 0, new ArrayList<>(), new ArrayList<>());
    }

    public BaseFlowNode(String id, String name, int order) {
        this(id, name, order, new ArrayList<>(), new ArrayList<>());
    }

    public BaseFlowNode(String id, String name, List<IFlowAction> actions) {
        this(id, name, 0, actions, new ArrayList<>());
    }

    public BaseFlowNode(String id, String name, int order, List<IFlowAction> actions, List<INodeStrategy> strategies) {
        this.id = id;
        this.name = name;
        this.order = order;
        this.actions = actions;
        this.strategies = strategies;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("type", getType());
        map.put("order", String.valueOf(order));
        map.put("actions", actions.stream().map(IFlowAction::toMap).toList());
        map.put("strategies", strategies.stream().map(INodeStrategy::toMap).toList());
        return map;
    }


    @SneakyThrows
    public static <T extends BaseFlowNode> T loadFromMap(Map<String, Object> map, Class<T> clazz) {
        T node = clazz.getDeclaredConstructor().newInstance();
        node.setId((String) map.get("id"));
        node.setName((String) map.get("name"));
        node.setOrder(Integer.parseInt((String) map.get("order")));
        node.setActions(NodeMapBuilder.loadActions(map));
        node.setStrategies(NodeMapBuilder.loadNodeStrategies(map));
        return node;
    }


    @Override
    public void verifyNode(FormMeta form) {

    }

    /**
     * 是否等待并行节点的汇聚
     */
    public boolean isWaitParallelRecord(FlowSession session) {
        FlowRecord currentRecord = session.getCurrentRecord();
        if (currentRecord != null && this.getId().equals(currentRecord.getParallelBranchNodeId())) {
            RepositoryContext.getInstance().addParallelTriggerCount(currentRecord.getParallelId());
            int parallelBranchTotal = currentRecord.getParallelBranchTotal();
            int parallelBranchCount = RepositoryContext.getInstance().getParallelBranchTriggerCount(currentRecord.getParallelId());
            if(parallelBranchCount == parallelBranchTotal){
                // 清空并行节点，防止数据继续继承到后续节点
                currentRecord.clearParallel();
            }
            return parallelBranchCount != parallelBranchTotal;
        }
        return false;
    }


    @Override
    public boolean continueTrigger(FlowSession session) {
        return true;
    }

    @Override
    public void verifySession(FlowSession session) {
        FlowAdvice flowAdvice = session.getAdvice();

        IFlowAction flowAction = flowAdvice.getAction();
        // 保存操作,不做检查
        if (flowAction instanceof SaveAction) {
            return;
        }
        // 转办操作
        if (flowAction instanceof TransferAction) {
            if (flowAdvice.getTransferOperators() == null || flowAdvice.getTransferOperators().isEmpty()) {
                throw new IllegalArgumentException("transferOperators can not be null");
            }
        }
        // 退回操作
        if (flowAction instanceof ReturnAction) {
            if (flowAdvice.getBackNode() == null) {
                throw new IllegalArgumentException("backNode can not be null");
            }
        }
    }

    @Override
    public boolean isDone(FlowSession session) {
        return true;
    }

    @Override
    public void fillNewRecord(FlowSession session, FlowRecord flowRecord) {

    }

    @Override
    public List<IFlowNode> filterBranches(List<IFlowNode> nodeList, FlowSession flowSession) {
        return nodeList;
    }

    @Override
    public List<FlowRecord> generateCurrentRecords(FlowSession session) {
        return List.of();
    }

    @Override
    public ActionManager actionManager() {
        return new ActionManager(actions);
    }

    @Override
    public StrategyManager strategyManager() {
        return new StrategyManager(strategies);
    }


}

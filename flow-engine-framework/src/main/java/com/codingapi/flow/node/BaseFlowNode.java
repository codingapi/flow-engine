package com.codingapi.flow.node;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.context.RepositoryContext;
import com.codingapi.flow.exception.FlowConfigException;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.builder.NodeMapBuilder;
import com.codingapi.flow.node.manager.ActionManager;
import com.codingapi.flow.node.manager.StrategyManager;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.INodeStrategy;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.util.StringUtils;

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


    /**
     * 节点策略
     * @param strategies 节点策略
     */
    public void setStrategies(List<INodeStrategy> strategies) {
        if (strategies != null && !strategies.isEmpty()) {
            if (this.strategies != null) {
                StrategyManager strategyManager = new StrategyManager(this.strategies);
                for (INodeStrategy nodeStrategy : strategies) {
                    INodeStrategy currentStrategy = strategyManager.getStrategy(nodeStrategy.getClass());
                    if (currentStrategy != null) {
                        currentStrategy.copy(nodeStrategy);
                    }
                }
            } else {
                this.strategies = strategies;
            }
        }
    }

    public void setActions(List<IFlowAction> actions) {
        if (actions != null && !actions.isEmpty()) {
            if (this.actions != null) {
                ActionManager actionManager = new ActionManager(this.actions);
                for (IFlowAction action : actions) {
                    IFlowAction currentAction = actionManager.getAction(action.getClass());
                    if (currentAction != null) {
                        currentAction.copy(action);
                    }else {
                        if(action instanceof CustomAction) {
                            this.actions.add(action);
                        }
                    }
                }
            } else {
                this.actions = actions;
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
        this.verifyDefaultConfig();
        ActionManager actionManager = this.actionManager();
        actionManager.verify(form);

        StrategyManager strategyManager = this.strategyManager();
        strategyManager.verify(form);
    }

    private void verifyDefaultConfig(){
        if (!StringUtils.hasText(name)) {
            throw FlowConfigException.nodeConfigError(name, "name can not be null");
        }
        if (!StringUtils.hasText(id)) {
            throw FlowConfigException.nodeConfigError(id, "id can not be null");
        }
        if (actions == null) {
            throw FlowConfigException.actionsNotNull();
        }
        if (strategies == null) {
            throw FlowConfigException.strategiesNotNull();
        }
    }

    /**
     * 是否等待并行节点的汇聚
     */
    public boolean isWaitRecordMargeParallelNode(FlowSession session) {
        FlowRecord currentRecord = session.getCurrentRecord();
        if (currentRecord != null && this.getId().equals(currentRecord.getParallelBranchNodeId())) {
            RepositoryContext.getInstance().addParallelTriggerCount(currentRecord.getParallelId());
            int parallelBranchTotal = currentRecord.getParallelBranchTotal();
            int parallelBranchCount = RepositoryContext.getInstance().getParallelBranchTriggerCount(currentRecord.getParallelId());
            if (parallelBranchCount == parallelBranchTotal) {
                // 清空并行节点，防止数据继续继承到后续节点
                currentRecord.clearParallel();
                RepositoryContext.getInstance().clearParallelTriggerCount(currentRecord.getParallelId());
            }
            return parallelBranchCount != parallelBranchTotal;
        }
        return false;
    }


    @Override
    public boolean handle(FlowSession session) {
        return true;
    }

    @Override
    public void verifySession(FlowSession session) {
        ActionManager actionManager = this.actionManager();
        actionManager.verifySession(session);

        StrategyManager strategyManager = this.strategyManager();
        strategyManager.verifySession(session);
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

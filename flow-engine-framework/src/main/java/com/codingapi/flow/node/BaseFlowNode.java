package com.codingapi.flow.node;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.builder.NodeMapBuilder;
import com.codingapi.flow.common.IMapConvertor;
import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.manager.ActionManager;
import com.codingapi.flow.manager.FlowNodeState;
import com.codingapi.flow.manager.NodeStrategyManager;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.HandleNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.node.INodeStrategy;
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
    @Getter
    protected List<IFlowAction> actions;

    /**
     * 节点策略
     */
    @Getter
    protected List<INodeStrategy> strategies;


    /**
     * 节点块
     */
    @Getter
    @Setter
    protected List<IFlowNode> blocks;


    /**
     * 节点策略
     *
     * @param strategies 节点策略
     */
    public void setStrategies(List<INodeStrategy> strategies) {
        if (strategies != null && !strategies.isEmpty()) {
            if (this.strategies != null) {
                NodeStrategyManager nodeStrategyManager = new NodeStrategyManager(this.strategies);
                for (INodeStrategy nodeStrategy : strategies) {
                    INodeStrategy currentStrategy = nodeStrategyManager.getStrategy(nodeStrategy.getClass());
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
                    } else {
                        if (action instanceof CustomAction) {
                            if (this.hasCustomAction()) {
                                this.actions.add(action);
                            }
                        }
                    }
                }
            } else {
                this.actions = actions;
            }
        }
    }


    /**
     * 是否可以包含自定义操作的节点，仅允许开始节点、审批节点、处理节点包含自定义操作
     */
    private boolean hasCustomAction() {
        return this.getType().equals(StartNode.NODE_TYPE) || this.getType().equals(ApprovalNode.NODE_TYPE) || this.getType().equals(HandleNode.NODE_TYPE);
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
        map.put("display",this instanceof IDisplayNode);
        map.put("order", String.valueOf(order));
        if (this.blocks != null && !this.blocks.isEmpty()) {
            map.put("blocks", blocks.stream().map(IFlowNode::toMap).toList());
        }
        map.put("actions", actions.stream().map(IFlowAction::toMap).toList());
        map.put("strategies", strategies.stream().map(INodeStrategy::toMap).toList());
        return map;
    }


    @SneakyThrows
    public static <T extends BaseFlowNode> T fromMap(Map<String, Object> map, Class<T> clazz) {
        T node = IMapConvertor.fromMap(map, clazz);
        node.setId((String) map.get("id"));
        node.setName((String) map.get("name"));
        if (map.get("order") != null) {
            node.setOrder(Integer.parseInt((String) map.get("order")));
        }
        node.setBlocks(NodeMapBuilder.loadNodes(map));
        node.setActions(NodeMapBuilder.loadActions(map));
        node.setStrategies(NodeMapBuilder.loadNodeStrategies(map));
        return node;
    }


    @Override
    public void verifyNode(FlowForm form) {
        this.verifyDefaultConfig();
        ActionManager actionManager = this.actionManager();
        actionManager.verify(form);

        NodeStrategyManager nodeStrategyManager = this.strategyManager();
        nodeStrategyManager.verifyNode(form);
    }

    private void verifyDefaultConfig() {
        if (!StringUtils.hasText(name)) {
            throw FlowValidationException.nodeRequired("name");
        }
        if (!StringUtils.hasText(id)) {
            throw FlowValidationException.nodeRequired("id");
        }
        if (actions == null) {
            throw FlowValidationException.nodeRequired("action");
        }
        if (strategies == null) {
            throw FlowValidationException.nodeRequired("strategies");
        }

        FlowNodeState nodeState = new FlowNodeState(this);
        if (nodeState.isBlockNode() || nodeState.isBranchNode()) {
            List<IFlowNode> blocks = this.blocks();
            if (blocks == null || blocks.isEmpty()) {
                throw FlowValidationException.nodeRequired("blocks");
            }
        }
    }

    /**
     * 是否等待并行节点的汇聚
     */
    public boolean isWaitRecordMargeParallelNode(FlowSession session) {
        FlowRecord currentRecord = session.getCurrentRecord();
        if (currentRecord != null && this.getId().equals(currentRecord.getParallelBranchNodeId())) {
            RepositoryHolderContext.getInstance().addParallelTriggerCount(currentRecord.getParallelId());
            int parallelBranchTotal = currentRecord.getParallelBranchTotal();
            int parallelBranchCount = RepositoryHolderContext.getInstance().getParallelBranchTriggerCount(currentRecord.getParallelId());
            if (parallelBranchCount == parallelBranchTotal) {
                // 清空并行节点，防止数据继续继承到后续节点
                currentRecord.clearParallel();
                RepositoryHolderContext.getInstance().clearParallelTriggerCount(currentRecord.getParallelId());
            }
            return parallelBranchCount != parallelBranchTotal;
        }
        return false;
    }


    /**
     * 匹配条件
     */
    @Override
    public boolean handle(FlowSession request) {
        // 是否等待并行合并节点
        if (this.isWaitRecordMargeParallelNode(request)) {
            return false;
        }
        return true;
    }

    @Override
    public void verifySession(FlowSession session) {
        ActionManager actionManager = this.actionManager();
        actionManager.verifySession(session);

        NodeStrategyManager nodeStrategyManager = this.strategyManager();
        nodeStrategyManager.verifySession(session);
    }

    @Override
    public boolean isFinish(FlowSession session) {
        return true;
    }

    @Override
    public void fillNewRecord(FlowSession session, FlowRecord flowRecord) {

    }

    @Override
    public List<IFlowNode> blocks() {
        return this.blocks;
    }

    @Override
    public List<IFlowNode> filterBranches(List<IFlowNode> nodeList, FlowSession flowSession) {
        return nodeList;
    }

    @Override
    public List<FlowRecord> generateCurrentRecords(FlowSession session) {
        return new ArrayList<>();
    }

    @Override
    public ActionManager actionManager() {
        return new ActionManager(actions);
    }

    @Override
    public NodeStrategyManager strategyManager() {
        return new NodeStrategyManager(strategies);
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BaseFlowNode node) {
            return node.getId().equals(id);
        }
        return super.equals(obj);
    }
}

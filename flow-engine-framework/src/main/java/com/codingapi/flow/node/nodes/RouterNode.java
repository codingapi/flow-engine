package com.codingapi.flow.node.nodes;

import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.exception.FlowExecutionException;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.manager.NodeStrategyManager;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.node.INodeStrategy;
import com.codingapi.flow.strategy.node.RouterStrategy;
import com.codingapi.flow.utils.RandomUtils;
import com.codingapi.flow.workflow.Workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 路由分支节点
 */
public class RouterNode extends BaseFlowNode {

    public static final String NODE_TYPE = NodeType.ROUTER.name();
    public static final String DEFAULT_NAME = "路由节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public RouterNode(String id, String name) {
        super(id, name,0, new ArrayList<>(),defaultStrategies());
    }

    public RouterNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME);
    }

    private static List<INodeStrategy> defaultStrategies() {
        List<INodeStrategy> strategies = new ArrayList<>();
        strategies.add(RouterStrategy.defaultStrategy());
        return strategies;
    }


    @Override
    public List<IFlowNode> filterBranches(List<IFlowNode> nodeList, FlowSession flowSession) {
        NodeStrategyManager nodeStrategyManager = this.strategyManager();
        RouterStrategy routerStrategy = nodeStrategyManager.getStrategy(RouterStrategy.class);
        String nextNodeId = routerStrategy.execute(flowSession);
        Workflow workflow = flowSession.getWorkflow();
        IFlowNode nextNode = workflow.getFlowNode(nextNodeId);
        if (nextNode == null) {
            throw FlowExecutionException.routerNodeNotFound(nextNodeId);
        }
        return List.of(nextNode);
    }


    public static RouterNode formMap(Map<String, Object> map) {
        return BaseFlowNode.fromMap(map, RouterNode.class);
    }

    @Override
    public void verifyNode(FormMeta form) {
        super.verifyNode(form);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, RouterNode> {
        public Builder() {
            super(new RouterNode());
        }
    }
}

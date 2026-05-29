package com.codingapi.flow.node.nodes;

import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.session.FlowSession;

import java.util.ArrayList;
import java.util.Map;

/**
 * 人工分支节点
 */
public class ManualBranchNode extends BaseFlowNode {

    public static final String NODE_TYPE = NodeType.MANUAL_BRANCH.name();
    public static final String DEFAULT_NAME = "人工分支节点";


    @Override
    public String getType() {
        return NODE_TYPE;
    }


    public static ManualBranchNode defaultNode(){
        ManualBranchNode manualBranchNode = new ManualBranchNode();
        manualBranchNode.setId(FlowIDGeneratorGatewayContext.getInstance().generateNodeId());
        manualBranchNode.setName(DEFAULT_NAME);
        manualBranchNode.setOrder(0);
        manualBranchNode.setActions(new ArrayList<>());
        manualBranchNode.setStrategies(new ArrayList<>());
        return manualBranchNode;
    }

    /**
     * 匹配条件
     */
    @Override
    public boolean handle(FlowSession request) {
        return true;
    }


    public static ManualBranchNode formMap(Map<String, Object> map) {
        return BaseFlowNode.fromMap(map, ManualBranchNode.class);
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, ManualBranchNode> {

        public Builder() {
            super(ManualBranchNode.defaultNode());
        }
    }
}

package com.codingapi.flow.node.nodes;

import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.IBlockNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.NodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 包容控制节点
 */
public class InclusiveNode extends BaseFlowNode implements IBlockNode {

    public static final String NODE_TYPE = NodeType.INCLUSIVE.name();
    public static final String DEFAULT_NAME = "包容节点";


    @Override
    public String getType() {
        return NODE_TYPE;
    }


    public static InclusiveNode defaultNode(){
        InclusiveNode inclusiveNode = new InclusiveNode();
        inclusiveNode.setId(FlowIDGeneratorGatewayContext.getInstance().generateNodeId());
        inclusiveNode.setName(DEFAULT_NAME);
        inclusiveNode.setOrder(0);
        inclusiveNode.setActions(new ArrayList<>());
        inclusiveNode.setStrategies(new ArrayList<>());
        return inclusiveNode;
    }


    @Override
    public void addDefaultBranch(int count){
        List<IFlowNode> branches = new ArrayList<>();
        for (int i=0;i<count;i++){
            InclusiveBranchNode branchNode = InclusiveBranchNode.defaultNode();
            branchNode.setOrder(i+1);
            branches.add(branchNode);
        }
        branches.add(InclusiveElseBranchNode.defaultNode());
        this.setBlocks(branches);
    }


    public static InclusiveNode formMap(Map<String, Object> map) {
        return BaseFlowNode.fromMap(map, InclusiveNode.class);
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, InclusiveNode> {

        public Builder() {
            super(InclusiveNode.defaultNode());
        }
    }
}

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
 * 并行控制
 */
public class ParallelNode extends BaseFlowNode implements IBlockNode {

    public static final String NODE_TYPE = NodeType.PARALLEL.name();
    public static final String DEFAULT_NAME = "并行节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }


    public static ParallelNode defaultNode(){
        ParallelNode parallelNode = new ParallelNode();
        parallelNode.setId(FlowIDGeneratorGatewayContext.getInstance().generateNodeId());
        parallelNode.setName(DEFAULT_NAME);
        parallelNode.setActions(new ArrayList<>());
        parallelNode.setStrategies(new ArrayList<>());
        return parallelNode;
    }

    @Override
    public void addDefaultBranch(int count){
        List<IFlowNode> branches = new ArrayList<>();
        for (int i=0;i<count;i++){
            ParallelBranchNode branchNode = new ParallelBranchNode();
            branchNode.setOrder(i+1);
            branches.add(branchNode);
        }
        this.setBlocks(branches);
    }


    public static ParallelNode formMap(Map<String, Object> map) {
        return BaseFlowNode.fromMap(map, ParallelNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, ParallelNode> {
        public Builder() {
            super(ParallelNode.defaultNode());
        }
    }
}

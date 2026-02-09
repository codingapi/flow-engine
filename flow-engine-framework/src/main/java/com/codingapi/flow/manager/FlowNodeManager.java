package com.codingapi.flow.manager;

import com.codingapi.flow.node.IFlowNode;

import java.util.List;

public class FlowNodeManager {

    private final List<IFlowNode> nodes;


    public FlowNodeManager(List<IFlowNode> nodes) {
        this.nodes = nodes;
    }

    public IFlowNode getFlowNode(String nodeId) {
        return this.loadFlowNode(nodeId, this.nodes);
    }


    private IFlowNode loadFlowNode(String nodeId,List<IFlowNode> nodes) {
        for (IFlowNode node : nodes) {
            if (node.getId().equals(nodeId)) {
                return node;
            }
            List<IFlowNode> blocks = node.blocks();
            if(blocks!=null && !blocks.isEmpty()){
                IFlowNode flowNode = loadFlowNode(nodeId,blocks);
                if(flowNode!=null){
                    return flowNode;
                }
            }
        }
        return null;
    }
}

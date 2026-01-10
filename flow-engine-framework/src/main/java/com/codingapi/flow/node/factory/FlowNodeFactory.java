package com.codingapi.flow.node.factory;

import com.codingapi.flow.node.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class FlowNodeFactory {

    @Getter
    private final static FlowNodeFactory instance = new FlowNodeFactory();

    private FlowNodeFactory(){
        this.init();
    }

    private final List<IFlowNode> nodes = new ArrayList<>();

    private void init(){
        this.addNode(new StartNode());
        this.addNode(new EndNode());
        this.addNode(new ApprovalNode());
        this.addNode(new ConditionBranchNode());
        this.addNode(new DelayNode());
        this.addNode(new HandleNode());
        this.addNode(new InclusiveBranchNode());
        this.addNode(new NotifyNode());
        this.addNode(new ParallelBranchNode());
        this.addNode(new RouterBranchNode());
        this.addNode(new SubProcessNode());
        this.addNode(new TriggerNode());
    }

    public void addNode(IFlowNode flowNode){
        nodes.add(flowNode);
    }

    public IFlowNode getNode(String type){
        for (IFlowNode IFlowNode : nodes) {
            if(IFlowNode.getType().equals(type)){
                return IFlowNode;
            }
        }
        return null;
    }

}

package com.codingapi.flow.node.helper;

import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.HandleNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.workflow.Workflow;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class BackNodeHelper {

    private final Workflow workflow;
    private final IFlowNode currentNode;

    @Getter
    private final List<IFlowNode> backNodes;

    private final List<String> backNodeTypes = new ArrayList<>();

    public BackNodeHelper(Workflow workflow, IFlowNode currentNode) {
        this.workflow = workflow;
        this.currentNode = currentNode;

        this.backNodes = new ArrayList<>();

        this.backNodeTypes.add(StartNode.NODE_TYPE);
        this.backNodeTypes.add(ApprovalNode.NODE_TYPE);
        this.backNodeTypes.add(HandleNode.NODE_TYPE);

        IFlowNode startNode = workflow.getStartNode();
        this.backNodes.add(startNode);

        this.fetchBackNodes(startNode);
    }


    private void fetchBackNodes(IFlowNode targetNode) {
        List<IFlowNode> nextNodes = workflow.nextNodes(targetNode);
        if (!nextNodes.contains(currentNode)) {
            for (IFlowNode nextNode : nextNodes) {
                if (this.backNodeTypes.contains(nextNode.getType())) {
                    this.backNodes.add(nextNode);
                }
                this.fetchBackNodes(nextNode);
            }
        }
    }

}

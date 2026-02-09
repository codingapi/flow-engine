package com.codingapi.flow.manager;

import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.NodeType;

import java.util.ArrayList;
import java.util.List;

/**
 *  TODO 节点关系管理,寻找下一节点的路径上存在bug
 */
public class FlowNodeEdgeManager {

    private final List<IFlowNode> nodes;

    private final List<String> blockNodeTypes = new ArrayList<>();
    private final List<String> branchNodeTypes = new ArrayList<>();

    public FlowNodeEdgeManager(List<IFlowNode> nodes) {
        this.nodes = nodes;

        this.blockNodeTypes.add(NodeType.CONDITION.name());
        this.blockNodeTypes.add(NodeType.INCLUSIVE.name());
        this.blockNodeTypes.add(NodeType.PARALLEL.name());

        this.branchNodeTypes.add(NodeType.CONDITION_BRANCH.name());
        this.branchNodeTypes.add(NodeType.INCLUSIVE_BRANCH.name());
        this.branchNodeTypes.add(NodeType.PARALLEL_BRANCH.name());
    }

    public List<IFlowNode> getNextNodes(IFlowNode current) {
        return this.loadNextNodes(current);
    }

    private List<IFlowNode> loadNextNodes(IFlowNode current) {
        if (isBlockNode(current)) {
            return current.blocks();
        }
        if (isBranchNode(current)) {
            List<IFlowNode> branchNodes = current.blocks();
            if (branchNodes != null && !branchNodes.isEmpty()) {
                return List.of(branchNodes.get(0));
            }
        }
        return this.fetchNextNode(current, this.nodes);
    }


    private boolean isBlockNode(IFlowNode node) {
        return blockNodeTypes.contains(node.getType());
    }

    private boolean isBranchNode(IFlowNode node) {
        return branchNodeTypes.contains(node.getType());
    }


    private List<IFlowNode> fetchNextNode(IFlowNode current, List<IFlowNode> nodes) {
        List<IFlowNode> nextNodes = new ArrayList<>();
        boolean match = false;

        for (int i = 0; i < nodes.size(); i++) {
            IFlowNode node = nodes.get(i);
            if (match) {
                nextNodes.add(node);
                break;
            }
            if (current.getId().equals(node.getId())) {
                match = true;
            }
            if (this.isBlockNode(node) || this.isBranchNode(node)) {
                List<IFlowNode> matchNodes = this.fetchNextNode(current, node.blocks());
                if (!matchNodes.isEmpty()) {
                    nextNodes.addAll(matchNodes);
                } else {
                    if (this.hasNodeBlocks(node, current)) {
                        nextNodes.add(nodes.get(i + 1));
                        break;
                    }
                }
            }
        }
        return nextNodes;
    }


    private boolean hasNodeBlocks(IFlowNode node, IFlowNode current) {
        List<IFlowNode> blocks = node.blocks();
        return blocks != null && !blocks.isEmpty() && hasNodeBlocks(current, blocks);
    }


    private boolean hasNodeBlocks(IFlowNode current, List<IFlowNode> nodes) {
        for (IFlowNode node : nodes) {
            if (node.getId().equals(current.getId())) {
                return true;
            }
            if (this.isBlockNode(node) || this.isBranchNode(node)) {
                if (hasNodeBlocks(current, node.blocks())) {
                    return true;
                }
            }
        }
        return false;
    }

}

package com.codingapi.flow.manager;

import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.NodeType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 流程节点状态
 */
public class FlowNodeState {

    @Getter
    private final IFlowNode node;

    private final List<String> blockNodeTypes = new ArrayList<>();
    private final List<String> branchNodeTypes = new ArrayList<>();

    public FlowNodeState(IFlowNode node) {
        this.node = node;

        this.blockNodeTypes.add(NodeType.CONDITION.name());
        this.blockNodeTypes.add(NodeType.INCLUSIVE.name());
        this.blockNodeTypes.add(NodeType.PARALLEL.name());

        this.branchNodeTypes.add(NodeType.CONDITION_BRANCH.name());
        this.branchNodeTypes.add(NodeType.INCLUSIVE_BRANCH.name());
        this.branchNodeTypes.add(NodeType.PARALLEL_BRANCH.name());
    }

    public boolean isEndNode(){
        return this.node.getType().equals(NodeType.END.name());
    }

    public boolean isBlockNode() {
        return blockNodeTypes.contains(node.getType());
    }

    public boolean isBranchNode() {
        return branchNodeTypes.contains(node.getType());
    }

    public List<IFlowNode> getBlocks() {
        return this.node.blocks();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FlowNodeState target) {
            return target.getId().equals(this.getId());
        }
        return super.equals(obj);
    }

    public String getId() {
        return this.node.getId();
    }


    public String getName() {
        return this.node.getName();
    }

    public List<IFlowNode> getFirstBlocks() {
        List<IFlowNode> blocks = this.node.blocks();
        if (blocks != null && !blocks.isEmpty()) {
            return Stream.of(blocks.get(0)).toList();
        }
        return new ArrayList<>();
    }
}

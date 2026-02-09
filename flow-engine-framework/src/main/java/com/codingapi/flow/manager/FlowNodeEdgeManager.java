package com.codingapi.flow.manager;

import com.codingapi.flow.exception.FlowConfigException;
import com.codingapi.flow.node.IFlowNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 流程节点关系管理器
 */
public class FlowNodeEdgeManager {

    private final Iterator<FlowNodeState> nodes;

    public FlowNodeEdgeManager(List<IFlowNode> nodes) {
        this.nodes = nodes.stream().map(FlowNodeState::new).iterator();
    }

    /**
     * 获取下一节点
     *
     * @param current 当前节点
     * @return 下一节点列表
     */
    public List<IFlowNode> getNextNodes(IFlowNode current) {
        return this.loadNextNodes(new FlowNodeState(current), this.nodes);
    }

    /**
     * 加载下一节点
     * @param current 当前节点状态
     * @param iterator  当前遍历的节点列表
     * @return 下一节点列表
     */
    private List<IFlowNode> loadNextNodes(FlowNodeState current, Iterator<FlowNodeState> iterator) {
        if(current.isEndNode()){
            return new ArrayList<>();
        }
        while (iterator.hasNext()) {
            FlowNodeState node = iterator.next();
            if (node.equals(current)) {
                if (node.isBlockNode()) {
                    return node.getBlocks();
                }
                if (node.isBranchNode()) {
                    return node.getFirstBlocks();
                }
                if (iterator.hasNext()) {
                    FlowNodeState next = iterator.next();
                    return Stream.of(next.getNode()).toList();
                } else {
                    // 跳过大循环，直接进入下一节点
                    if (this.nodes.hasNext()) {
                        return Stream.of(this.nodes.next().getNode()).toList();
                    }else {
                        throw FlowConfigException.edgeConfigError(current.getName());
                    }
                }
            }

            if (node.isBlockNode() || node.isBranchNode()) {
                List<IFlowNode> nextNodes = this.loadNextNodes(current, node.getBlocks().stream().map(FlowNodeState::new).toList().iterator());
                if (!nextNodes.isEmpty()) {
                    return nextNodes;
                }
            }

        }
        return new ArrayList<>();
    }

}

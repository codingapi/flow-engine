package com.codingapi.flow.manager;

import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.node.IFlowNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 流程节点管理器，主要获取流程节点信息
 */
public class FlowNodeManager {

    private final List<IFlowNode> nodes;

    public FlowNodeManager(List<IFlowNode> nodes) {
        this.nodes = nodes;
    }

    public IFlowNode getFlowNode(String nodeId) {
        return this.fetchNodes(nodeId, this.nodes);
    }

    private IFlowNode fetchNodes(String nodeId, List<IFlowNode> nodes) {
        for (IFlowNode node : nodes) {
            if (node.getId().equals(nodeId)) {
                return node;
            }
            List<IFlowNode> blocks = node.blocks();
            if (blocks != null && !blocks.isEmpty()) {
                IFlowNode flowNode = this.fetchNodes(nodeId, blocks);
                if (flowNode != null) {
                    return flowNode;
                }
            }
        }
        return null;
    }

    /**
     * 获取下一节点
     *
     * @param current 当前节点
     * @return 下一节点列表
     */
    public List<IFlowNode> getNextNodes(IFlowNode current) {
        FlowNodeSearcher edgeManager = new FlowNodeSearcher(this.nodes);
        return edgeManager.getNextNodes(current);
    }


    /**
     * 流程节点关系管理器，主要分析下一节点
     */
    private static class FlowNodeSearcher {

        private final Iterator<FlowNodeState> nodes;

        public FlowNodeSearcher(List<IFlowNode> nodes) {
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
         *
         * @param current  当前节点状态
         * @param iterator 当前遍历的节点列表
         * @return 下一节点列表
         */
        private List<IFlowNode> loadNextNodes(FlowNodeState current, Iterator<FlowNodeState> iterator) {
            if (current.isEndNode()) {
                return new ArrayList<>();
            }
            while (iterator.hasNext()) {
                FlowNodeState node = iterator.next();
                if (node.equals(current)) {
                    if (node.isBlockNode()) {
                        return node.getBlocks();
                    }
                    if (node.isBranchNode()) {
                        List<IFlowNode> nextNodes = node.getFirstBlocks();
                        if(!nextNodes.isEmpty()){
                            return nextNodes;
                        }else {
                            // 跳过大循环，直接进入下一节点
                            if (this.nodes.hasNext()) {
                                return Stream.of(this.nodes.next().getNode()).toList();
                            } else {
                                throw FlowStateException.edgeConfigError(current.getName());
                            }
                        }
                    }
                    if (iterator.hasNext()) {
                        FlowNodeState next = iterator.next();
                        return Stream.of(next.getNode()).toList();
                    } else {
                        // 跳过大循环，直接进入下一节点
                        if (this.nodes.hasNext()) {
                            return Stream.of(this.nodes.next().getNode()).toList();
                        } else {
                            throw FlowStateException.edgeConfigError(current.getName());
                        }
                    }
                }

                if (node.isBlockNode() || node.isBranchNode()) {
                    if (node.getBlocks() != null) {
                        List<IFlowNode> nextNodes = this.loadNextNodes(current, node.getBlocks().stream().map(FlowNodeState::new).toList().iterator());
                        if (!nextNodes.isEmpty()) {
                            return nextNodes;
                        }
                    }
                }
            }
            return new ArrayList<>();
        }

    }
}

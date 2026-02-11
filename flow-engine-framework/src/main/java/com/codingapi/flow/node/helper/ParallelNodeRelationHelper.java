package com.codingapi.flow.node.helper;

import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.workflow.Workflow;

import java.util.List;

/**
 * 并行节点需要在最后的合并的节点上统一触发下一流程，因此需要分析最终汇聚的节点
 * <p>
 */
public class ParallelNodeRelationHelper {
    private final Workflow workflow;
    private final IFlowNode currentNode;
    private final List<IFlowNode> parallelNodes;

    public ParallelNodeRelationHelper(Workflow workflow, IFlowNode currentNode, List<IFlowNode> parallelNodes) {
        this.parallelNodes = parallelNodes;
        this.currentNode = currentNode;
        this.workflow = workflow;
    }

    /**
     * 获取合并节点
     */
    public IFlowNode fetchMargeNode() {
        if (parallelNodes.isEmpty()) {
            return null;
        }

        IFlowNode overBlockNode = loadOverBlockNodes();
        List<IFlowNode> nextNodes = workflow.nextNodes(overBlockNode);
        if (nextNodes != null && nextNodes.size() == 1) {
            return nextNodes.get(0);
        }
        return null;
    }


    /**
     * 获取最后的block节点
     */
    public IFlowNode loadOverBlockNodes() {
        IFlowNode node = currentNode;
        List<IFlowNode> blocks = node.blocks();
        while (blocks != null && !blocks.isEmpty()) {
            int size = blocks.size();
            node = blocks.get(size - 1);
            blocks = node.blocks();
        }
        return node;
    }

}

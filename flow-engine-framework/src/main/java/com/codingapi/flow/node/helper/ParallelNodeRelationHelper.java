package com.codingapi.flow.node.helper;

import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.workflow.Workflow;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 并行节点需要在最后的合并的节点上统一触发下一流程，因此需要分析最终汇聚的节点
 *
 * TODO 获取合并节点的算法，可直接进入大循环的下一个节点即可得到
 * TODO 合并节点，也未必是一个节点，可能是条件、并行、包容、子流程等这些无审批记录的节点上
 */
public class ParallelNodeRelationHelper {
    private final Workflow workflow;
    private final List<IFlowNode> parallelNodes;

    public ParallelNodeRelationHelper(List<IFlowNode> parallelNodes, Workflow workflow) {
        this.parallelNodes = parallelNodes;
        this.workflow = workflow;
    }

    public IFlowNode fetchParallelEndNode() {
        if (parallelNodes.isEmpty()) {
            return null;
        }
        if (parallelNodes.size() > 1) {
            ParallelNodeRelationHelper.LineManager lineManager = new ParallelNodeRelationHelper.LineManager();
            for (IFlowNode node : parallelNodes) {
                List<String> nodeLines = this.getNodeLines(node);
                lineManager.addLine(nodeLines);
            }
            return lineManager.fetchEndNode(workflow);
        }
        return parallelNodes.get(0);
    }

    private List<String> getNodeLines(IFlowNode node) {
        List<String> lines = new ArrayList<>();
        lines.add(node.getId());
        IFlowNode currentNode = node;
        ParallelNodeRelationHelper.NodeManger nodeManger = null;
        do {
            nodeManger = this.nextNodes(currentNode);
            currentNode = nodeManger.getCurrentNode();
            lines.add(currentNode.getId());
        } while (nodeManger.next());
        return lines;
    }

    private ParallelNodeRelationHelper.NodeManger nextNodes(IFlowNode node) {
        return new ParallelNodeRelationHelper.NodeManger(workflow.nextNodes(node));
    }

    private static class LineManager {

        private final List<List<String>> lines = new ArrayList<>();

        public void addLine(List<String> line) {
            lines.add(line);
        }


        public IFlowNode fetchEndNode(Workflow workflow) {
            // 对线进行倒叙
            List<String> firstLine = lines.get(0);
            Collections.reverse(firstLine);

            IFlowNode flowNode = null;
            for (int i = 1; i < lines.size(); i++) {
                List<String> line = lines.get(i);
                if (flowNode == null) {
                    for (String nodeId : firstLine) {
                        if (line.contains(nodeId)) {
                            flowNode = workflow.getFlowNode(nodeId);
                        }
                    }
                }
            }
            return flowNode;
        }

    }


    @AllArgsConstructor
    private static class NodeManger {
        @Getter
        private final List<IFlowNode> nodes;

        public IFlowNode getCurrentNode() {
            return nodes.get(0);
        }

        public boolean next() {
            if (nodes.isEmpty()) {
                return false;
            }
            IFlowNode currentNode = nodes.get(0);
            if (currentNode instanceof EndNode) {
                return false;
            }
            return true;
        }
    }
}

package com.codingapi.flow.node.nodes;

import com.codingapi.flow.exception.FlowConfigException;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;
import com.codingapi.flow.workflow.Workflow;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;

/**
 * 并行节点
 */
public class ParallelBranchNode extends BaseFlowNode {

    public static final String NODE_TYPE = "parallel_branch";
    public static final String DEFAULT_NAME = "并行节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public ParallelBranchNode(String id, String name) {
        super(id, name);
    }

    public ParallelBranchNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME);
    }

    /**
     * 匹配条件分支
     *
     * @param nodeList    当前节点下的所有条件
     * @param flowSession 当前会话
     * @return 匹配的节点
     */
    public List<IFlowNode> filterBranches(List<IFlowNode> nodeList, FlowSession flowSession) {
        Workflow workflow = flowSession.getWorkflow();
        ParallelNodeRelationHelper helper = new ParallelNodeRelationHelper(nodeList, workflow);
        // 分析并行分支的结束汇聚节点
        IFlowNode overNode = helper.fetchParallelEndNode();
        if(overNode==null){
            throw FlowConfigException.parallelEndNodeNotNull();
        }

        // 在流程记录中记录，合并的条件信息。
        FlowRecord flowRecord = flowSession.getCurrentRecord();
        flowRecord.parallelBranchNode(overNode.getId(), nodeList.size(),RandomUtils.generateStringId());

        return nodeList;
    }


    private static class ParallelNodeRelationHelper {
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
                LineManager lineManager = new LineManager();
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
            NodeManger nodeManger = null;
            do {
                nodeManger = this.nextNodes(currentNode);
                currentNode = nodeManger.getCurrentNode();
                lines.add(currentNode.getId());
            } while (nodeManger.next());
            return lines;
        }

        private NodeManger nextNodes(IFlowNode node) {
            return new NodeManger(workflow.nextNodes(node));
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


    public static ParallelBranchNode formMap(Map<String, Object> map) {
        return BaseFlowNode.loadFromMap(map, ParallelBranchNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, ParallelBranchNode> {
        public Builder() {
            super(new ParallelBranchNode());
        }
    }
}

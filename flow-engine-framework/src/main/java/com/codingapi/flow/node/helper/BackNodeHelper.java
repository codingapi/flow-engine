package com.codingapi.flow.node.helper;

import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.HandleNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.workflow.Workflow;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BackNodeHelper {

    /**
     * 允许作为退回目标的节点类型：只有会产生真实审批记录的节点才可退回，
     * 抄送、子流程、路由等节点退出时会静默停滞，不参与退回。
     */
    private static final Set<String> BACK_NODE_TYPES = Set.of(
            StartNode.NODE_TYPE,
            ApprovalNode.NODE_TYPE,
            HandleNode.NODE_TYPE);

    private final Workflow workflow;
    private final IFlowNode currentNode;

    @Getter
    private final List<IFlowNode> backNodes;

    public BackNodeHelper(Workflow workflow, IFlowNode currentNode) {
        this.workflow = workflow;
        this.currentNode = currentNode;

        this.backNodes = new ArrayList<>();

        IFlowNode startNode = workflow.getStartNode();
        this.backNodes.add(startNode);

        this.fetchBackNodes(startNode);
    }


    private void fetchBackNodes(IFlowNode targetNode) {
        List<IFlowNode> nextNodes = workflow.nextNodes(targetNode);
        if (!nextNodes.contains(currentNode)) {
            for (IFlowNode nextNode : nextNodes) {
                if (BACK_NODE_TYPES.contains(nextNode.getType())) {
                    this.backNodes.add(nextNode);
                }
                this.fetchBackNodes(nextNode);
            }
        }
    }

    /**
     * 按真实执行历史计算可退回节点。
     *
     * <p>退回只能回到当前记录实际经过的节点，因此候选来源是当前记录之前的历史记录，
     * 而不是流程定义的全量节点（issue #270）：</p>
     * <ul>
     *     <li>历史记录按记录 id 正序排列后<b>倒序</b>遍历，保证“最近经过的节点”排在第一位
     *     （例如停留在 C 节点时得到 B、A）；</li>
     *     <li>同一节点多次执行（退回后重新提交）只保留最近一次，避免重复选项；</li>
     *     <li>当前节点自身不可作为退回目标，未执行过的分支节点自然不会出现。</li>
     * </ul>
     *
     * @param workflow      当前流程定义
     * @param beforeRecords 当前记录之前的历史记录（可乱序）
     * @param currentNodeId 当前记录所在节点 id
     * @return 可退回节点，最近优先
     */
    public static List<IFlowNode> historyBackNodes(Workflow workflow,
                                                   List<FlowRecord> beforeRecords,
                                                   String currentNodeId) {
        if (beforeRecords == null || beforeRecords.isEmpty()) {
            return new ArrayList<>();
        }
        List<FlowRecord> sortedRecords = beforeRecords.stream()
                .sorted(Comparator.comparingLong(FlowRecord::getId))
                .toList();

        List<IFlowNode> historyBackNodes = new ArrayList<>();
        Set<String> visitedNodeIds = new HashSet<>();
        for (int i = sortedRecords.size() - 1; i >= 0; i--) {
            String nodeId = sortedRecords.get(i).getNodeId();
            if (!visitedNodeIds.add(nodeId)) {
                continue;
            }
            if (nodeId.equals(currentNodeId)) {
                continue;
            }
            IFlowNode node = workflow.getFlowNode(nodeId);
            if (node == null || !BACK_NODE_TYPES.contains(node.getType())) {
                continue;
            }
            historyBackNodes.add(node);
        }
        return historyBackNodes;
    }

}

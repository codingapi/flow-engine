package com.codingapi.flow.node.nodes;

import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.exception.FlowConfigException;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.node.helper.ParallelNodeRelationHelper;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;
import com.codingapi.flow.workflow.Workflow;

import java.util.List;
import java.util.Map;

/**
 * 并行节点
 */
public class ParallelBranchNode extends BaseFlowNode {

    public static final String NODE_TYPE = NodeType.PARALLEL_BRANCH.name();
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
        if (overNode == null) {
            throw FlowConfigException.parallelEndNodeNotNull();
        }

        // 在流程记录中记录，合并的条件信息。
        FlowRecord flowRecord = flowSession.getCurrentRecord();
        flowRecord.parallelBranchNode(overNode.getId(), nodeList.size(), RandomUtils.generateStringId());

        return nodeList;
    }


    public static ParallelBranchNode formMap(Map<String, Object> map) {
        return BaseFlowNode.fromMap(map, ParallelBranchNode.class);
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

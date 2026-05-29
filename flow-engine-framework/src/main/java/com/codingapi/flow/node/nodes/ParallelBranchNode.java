package com.codingapi.flow.node.nodes;

import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.node.helper.ParallelNodeRelationHelper;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.workflow.Workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 并行分支节点
 */
public class ParallelBranchNode extends BaseFlowNode {

    public static final String NODE_TYPE = NodeType.PARALLEL_BRANCH.name();
    public static final String DEFAULT_NAME = "并行分支节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public static ParallelBranchNode defaultNode(){
        ParallelBranchNode parallelBranchNode = new ParallelBranchNode();
        parallelBranchNode.setId(FlowIDGeneratorGatewayContext.getInstance().generateNodeId());
        parallelBranchNode.setName(DEFAULT_NAME);
        parallelBranchNode.setActions(new ArrayList<>());
        parallelBranchNode.setStrategies(new ArrayList<>());
        return parallelBranchNode;
    }

    @Override
    public boolean handle(FlowSession request) {
        return true;
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
        IFlowNode currentNode = flowSession.getCurrentNode();
        ParallelNodeRelationHelper helper = new ParallelNodeRelationHelper(workflow, currentNode, nodeList);
        // 分析并行分支的结束汇聚节点
        IFlowNode overNode = helper.fetchMargeNode();
        if (overNode == null) {
            throw FlowNotFoundException.parallelEndNodeNotNull();
        }

        // 在流程记录中记录，合并的条件信息。
        FlowRecord flowRecord = flowSession.getCurrentRecord();
        flowRecord.parallelBranchNode(overNode.getId(), nodeList.size(),FlowIDGeneratorGatewayContext.getInstance().generateParallelId());

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
            super(ParallelBranchNode.defaultNode());
        }
    }
}

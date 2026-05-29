package com.codingapi.flow.node.nodes;

import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.node.helper.ParallelNodeRelationHelper;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.node.ConditionScript;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.workflow.Workflow;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 包容分支节点
 */
public class InclusiveBranchNode extends BaseFlowNode {

    public static final String NODE_TYPE = NodeType.INCLUSIVE_BRANCH.name();
    public static final String DEFAULT_NAME = "包容分支节点";

    /**
     * 条件脚本
     */
    @Setter
    private ConditionScript conditionScript;

    @Override
    public String getType() {
        return NODE_TYPE;
    }


    public static InclusiveBranchNode defaultNode(){
        InclusiveBranchNode inclusiveBranchNode = new InclusiveBranchNode();
        inclusiveBranchNode.setId(FlowIDGeneratorGatewayContext.getInstance().generateNodeId());
        inclusiveBranchNode.setName(DEFAULT_NAME);
        inclusiveBranchNode.setOrder(0);
        inclusiveBranchNode.conditionScript = ConditionScript.defaultScript();
        inclusiveBranchNode.setActions(new ArrayList<>());
        inclusiveBranchNode.setStrategies(new ArrayList<>());
        return inclusiveBranchNode;
    }


    /**
     * 匹配条件
     */
    @Override
    public boolean handle(FlowSession request) {
        return conditionScript.execute(request);
    }


    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("script", conditionScript.getScript());
        return map;
    }

    public static InclusiveBranchNode formMap(Map<String, Object> map) {
        InclusiveBranchNode branchNode = BaseFlowNode.fromMap(map, InclusiveBranchNode.class);
        branchNode.conditionScript = new ConditionScript((String) map.get("script"));
        return branchNode;
    }


    private List<IFlowNode> filterNodes(List<IFlowNode> nodeList, FlowSession flowSession){
        List<IFlowNode> nodes = new ArrayList<>();
        for (IFlowNode node : nodeList) {
            if (node.handle(flowSession)) {
                nodes.add(node);
            }
        }
        // 获取移除else节点
        if (!nodes.isEmpty() && nodes.size()>2) {
            List<IFlowNode> filterNodes = new ArrayList<>();
            for (IFlowNode node : nodes){
                if(!node.getType().equalsIgnoreCase(NodeType.INCLUSIVE_ELSE_BRANCH.name())){
                    filterNodes.add(node);
                }
            }
            return filterNodes;
        }
        return nodes;
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

        List<IFlowNode> nodes = this.filterNodes(nodeList,flowSession);
        ParallelNodeRelationHelper helper = new ParallelNodeRelationHelper(workflow,currentNode,nodes);
        // 分析并行分支的结束汇聚节点
        IFlowNode overNode = helper.fetchMargeNode();
        if (overNode == null) {
            throw FlowNotFoundException.parallelEndNodeNotNull();
        }

        // 在流程记录中记录，合并的条件信息。
        FlowRecord flowRecord = flowSession.getCurrentRecord();
        flowRecord.parallelBranchNode(overNode.getId(), nodes.size(), FlowIDGeneratorGatewayContext.getInstance().generateParallelId());

        return nodes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, InclusiveBranchNode> {

        public Builder() {
            super(InclusiveBranchNode.defaultNode());
        }

        public Builder conditionScript(String script) {
            node.conditionScript = new ConditionScript(script);
            return this;
        }
    }
}

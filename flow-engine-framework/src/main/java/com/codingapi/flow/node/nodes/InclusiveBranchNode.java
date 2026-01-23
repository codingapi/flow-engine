package com.codingapi.flow.node.nodes;

import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.exception.FlowConfigException;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.node.helper.ParallelNodeRelationHelper;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.node.ConditionScript;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;
import com.codingapi.flow.workflow.Workflow;
import lombok.Setter;

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


    public InclusiveBranchNode(String id, String name, int order) {
        super(id, name, order);
        conditionScript = ConditionScript.defaultScript();
    }

    public InclusiveBranchNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, 0);
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
        InclusiveBranchNode branchNode = BaseFlowNode.loadFromMap(map, InclusiveBranchNode.class);
        branchNode.conditionScript = new ConditionScript((String) map.get("script"));
        return branchNode;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, InclusiveBranchNode> {

        public Builder() {
            super(new InclusiveBranchNode());
        }

        public Builder conditionScript(String script) {
            node.conditionScript = new ConditionScript(script);
            return this;
        }
    }
}

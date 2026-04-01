package com.codingapi.flow.node.nodes;

import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.exception.FlowNotFoundException;
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
 * 人工分支节点
 */
public class ManualBranchNode extends BaseFlowNode {

    public static final String NODE_TYPE = NodeType.MANUAL_BRANCH.name();
    public static final String DEFAULT_NAME = "人工分支节点";


    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public ManualBranchNode(String id, String name, int order) {
        super(id, name, order);
    }

    public ManualBranchNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, 0);
    }

    /**
     * 匹配条件
     */
    @Override
    public boolean handle(FlowSession request) {
        return true;
    }


    public static ManualBranchNode formMap(Map<String, Object> map) {
        return BaseFlowNode.fromMap(map, ManualBranchNode.class);
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, ManualBranchNode> {

        public Builder() {
            super(new ManualBranchNode());
        }
    }
}

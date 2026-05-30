package com.codingapi.flow.node.nodes;

import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.node.*;
import com.codingapi.flow.session.FlowSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 人工控制节点
 */
public class ManualNode extends BaseFlowNode implements IBlockNode , IDisplayNode {

    public static final String NODE_TYPE = NodeType.MANUAL.name();
    public static final String DEFAULT_NAME = "人工节点";


    @Override
    public String getType() {
        return NODE_TYPE;
    }


    public static ManualNode defaultNode(){
        ManualNode manualNode = new ManualNode();
        manualNode.setId(FlowIDGeneratorGatewayContext.getInstance().generateNodeId());
        manualNode.setName(DEFAULT_NAME);
        manualNode.setOrder(0);
        manualNode.setActions(new ArrayList<>());
        manualNode.setStrategies(new ArrayList<>());
        return manualNode;
    }

    /**
     * 匹配条件分支
     *
     * @param nodeList    当前节点下的所有条件
     * @param flowSession 当前会话
     * @return 匹配的节点
     */
    public List<IFlowNode> filterBranches(List<IFlowNode> nodeList, FlowSession flowSession) {
        IFlowNode selectNode = flowSession.getAdvice().getManualNode();
        if (selectNode == null) {
            return nodeList;
        } else {
            List<IFlowNode> nextNodes = new ArrayList<>();
            nextNodes.add(selectNode);
            return nextNodes;
        }
    }

    @Override
    public void addDefaultBranch(int count) {
        List<IFlowNode> branches = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ManualBranchNode branchNode = ManualBranchNode.defaultNode();
            branchNode.setOrder(i + 1);
            branches.add(branchNode);
        }
        this.setBlocks(branches);
    }


    public static ManualNode formMap(Map<String, Object> map) {
        return BaseFlowNode.fromMap(map, ManualNode.class);
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, ManualNode> {

        public Builder() {
            super(ManualNode.defaultNode());
        }
    }
}

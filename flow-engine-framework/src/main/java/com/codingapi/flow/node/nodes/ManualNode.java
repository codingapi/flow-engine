package com.codingapi.flow.node.nodes;

import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.IBlockNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 人工控制节点
 */
public class ManualNode extends BaseFlowNode implements IBlockNode {

    public static final String NODE_TYPE = NodeType.MANUAL.name();
    public static final String DEFAULT_NAME = "人工控制节点";


    @Override
    public String getType() {
        return NODE_TYPE;
    }


    public ManualNode(String id, String name, int order) {
        super(id, name, order);
    }

    public ManualNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, 0);
    }


    @Override
    public void addDefaultBranch(int count){
        List<IFlowNode> branches = new ArrayList<>();
        for (int i=0;i<count;i++){
            ManualBranchNode branchNode = new ManualBranchNode();
            branchNode.setOrder(i+1);
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
            super(new ManualNode());
        }
    }
}

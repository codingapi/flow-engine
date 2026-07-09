package com.codingapi.flow.node.factory;

import com.alibaba.fastjson.JSON;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.StartNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NodeFactoryTest {

    @Test
    void createApprovalNode() {
        IFlowNode node = NodeFactory.getInstance().createNode(NodeType.APPROVAL);
        assertNotNull(node);
        assertEquals(node.getType(), NodeType.APPROVAL.name());
    }

    @Test
    void createConditionBranchNode() {
        IFlowNode node = NodeFactory.getInstance().createNode(NodeType.CONDITION_BRANCH);
        assertNotNull(node);
        assertEquals(node.getType(), NodeType.CONDITION_BRANCH.name());
    }

    @Test
    void createDelayNode() {
        IFlowNode node = NodeFactory.getInstance().createNode(NodeType.DELAY);
        assertNotNull(node);
        assertEquals(node.getType(), NodeType.DELAY.name());
    }

    @Test
    void createEndNode() {
        IFlowNode node = NodeFactory.getInstance().createNode(NodeType.END);
        assertNotNull(node);
        assertEquals(node.getType(), NodeType.END.name());
    }


    @Test
    void createHandleNode() {
        IFlowNode node = NodeFactory.getInstance().createNode(NodeType.HANDLE);
        assertNotNull(node);
        assertEquals(node.getType(), NodeType.HANDLE.name());
    }

    @Test
    void createInclusiveBranchNode() {
        IFlowNode node = NodeFactory.getInstance().createNode(NodeType.INCLUSIVE_BRANCH);
        assertNotNull(node);
        assertEquals(node.getType(), NodeType.INCLUSIVE_BRANCH.name());
    }

    @Test
    void createNotifyNode() {
        IFlowNode node = NodeFactory.getInstance().createNode(NodeType.NOTIFY);
        assertNotNull(node);
        assertEquals(node.getType(), NodeType.NOTIFY.name());
    }

    @Test
    void createParallelBranchNode() {
        IFlowNode node = NodeFactory.getInstance().createNode(NodeType.PARALLEL_BRANCH);
        assertNotNull(node);
        assertEquals(node.getType(), NodeType.PARALLEL_BRANCH.name());
    }

    @Test
    void createRouterNode() {
        IFlowNode node = NodeFactory.getInstance().createNode(NodeType.ROUTER);
        assertNotNull(node);
        assertEquals(node.getType(), NodeType.ROUTER.name());
    }


    @Test
    void createStartNode() {
        IFlowNode node = NodeFactory.getInstance().createNode(NodeType.START);
        assertNotNull(node);
        assertEquals(node.getType(), NodeType.START.name());
    }

    @Test
    void shouldConvertViewTitle() {
        StartNode startNode = StartNode.builder()
                .name("发起节点")
                .build();
        startNode.setViewTitle("发起视图");

        Map<String, Object> data = JSON.parseObject(JSON.toJSONString(startNode.toMap()));
        IFlowNode node = NodeFactory.getInstance().createNode(data);

        assertNotNull(node);
        StartNode restoredNode = (StartNode) node;
        assertEquals("发起视图", restoredNode.getViewTitle());
        assertEquals("发起视图", restoredNode.toMap().get("viewTitle"));
    }

    @Test
    void shouldFallbackViewTitleWhenImportOldNodeData() {
        ApprovalNode approvalNode = ApprovalNode.builder()
                .name("经理审批")
                .build();

        Map<String, Object> data = JSON.parseObject(JSON.toJSONString(approvalNode.toMap()));
        data.remove("viewTitle");

        IFlowNode node = NodeFactory.getInstance().createNode(data);

        assertNotNull(node);
        ApprovalNode restoredNode = (ApprovalNode) node;
        assertEquals("经理审批", restoredNode.getViewTitle());
        assertEquals("经理审批", restoredNode.toMap().get("viewTitle"));
    }


    @Test
    void createSubProcessNode() {
        IFlowNode node = NodeFactory.getInstance().createNode(NodeType.SUB_PROCESS);
        assertNotNull(node);
        assertEquals(node.getType(), NodeType.SUB_PROCESS.name());
    }


    @Test
    void createTriggerNode() {
        IFlowNode node = NodeFactory.getInstance().createNode(NodeType.TRIGGER);
        assertNotNull(node);
        assertEquals(node.getType(), NodeType.TRIGGER.name());
    }
}

package com.codingapi.flow.node.factory;

import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.NodeType;
import org.junit.jupiter.api.Test;

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
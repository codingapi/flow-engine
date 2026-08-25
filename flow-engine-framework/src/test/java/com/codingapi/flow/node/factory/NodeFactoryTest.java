package com.codingapi.flow.node.factory;

import com.alibaba.fastjson.JSON;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.node.nodes.SubProcessNode;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.strategy.node.SubProcessStrategy;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void shouldKeepViewTitleNullWhenNotSet() {
        ApprovalNode approvalNode = ApprovalNode.builder()
                .name("经理审批")
                .build();

        Map<String, Object> data = JSON.parseObject(JSON.toJSONString(approvalNode.toMap()));

        IFlowNode node = NodeFactory.getInstance().createNode(data);

        assertNotNull(node);
        ApprovalNode restoredNode = (ApprovalNode) node;
        assertNull(restoredNode.getViewTitle());
        assertNull(restoredNode.toMap().get("viewTitle"));
    }


    @Test
    void createSubProcessNode() {
        IFlowNode node = NodeFactory.getInstance().createNode(NodeType.SUB_PROCESS);
        assertNotNull(node);
        assertEquals(node.getType(), NodeType.SUB_PROCESS.name());
    }

    @Test
    void shouldPreserveSubProcessConfigurationWhenConvertingNode() {
        SubProcessNode source = SubProcessNode.builder()
                .name("子流程")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy(
                                "create-script", false, "result-script", true, true))
                        .build())
                .build();
        Map<String, Object> data = JSON.parseObject(JSON.toJSONString(source.toMap()));

        SubProcessNode restored = (SubProcessNode) NodeFactory.getInstance().createNode(data);
        SubProcessStrategy strategy = restored.strategyManager().getStrategy(SubProcessStrategy.class);

        assertNotNull(strategy);
        assertEquals("create-script", strategy.getSubProcessScript().getScript());
        assertEquals("result-script", strategy.getResultScript().getScript());
        assertEquals(false, strategy.isSubmit());
        assertTrue(strategy.isShowParentProcessRecords());
        assertTrue(strategy.isResettable());
    }

    /**
     * 测试目标：验证历史流程定义没有主流程记录展示配置时保持默认关闭。
     * 前置条件：子流程策略 Map 中不包含 showParentProcessRecords。
     * 执行步骤：通过节点工厂反序列化历史节点定义。
     * 期望断言：主流程记录展示开关为 false。
     */
    @Test
    void shouldDisableParentProcessRecordsForLegacySubProcessConfiguration() {
        SubProcessNode source = SubProcessNode.builder()
                .name("历史子流程")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy("create-script", true, "result-script"))
                        .build())
                .build();
        Map<String, Object> data = JSON.parseObject(JSON.toJSONString(source.toMap()));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> strategies = (List<Map<String, Object>>) data.get("strategies");
        strategies.get(0).remove("showParentProcessRecords");

        SubProcessNode restored = (SubProcessNode) NodeFactory.getInstance().createNode(data);
        SubProcessStrategy restoredStrategy = restored.strategyManager().getStrategy(SubProcessStrategy.class);

        assertNotNull(restoredStrategy);
        assertFalse(restoredStrategy.isShowParentProcessRecords());
    }

    /**
     * 测试目标：验证历史流程定义没有重置能力配置时保持默认关闭（issue #219）。
     * 前置条件：子流程策略 Map 中不包含 resettable。
     * 执行步骤：通过节点工厂反序列化历史节点定义。
     * 期望断言：重置能力开关为 false。
     */
    @Test
    void shouldDisableResettableForLegacySubProcessConfiguration() {
        SubProcessNode source = SubProcessNode.builder()
                .name("历史子流程")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy("create-script", true, "result-script"))
                        .build())
                .build();
        Map<String, Object> data = JSON.parseObject(JSON.toJSONString(source.toMap()));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> strategies = (List<Map<String, Object>>) data.get("strategies");
        strategies.get(0).remove("resettable");

        SubProcessNode restored = (SubProcessNode) NodeFactory.getInstance().createNode(data);
        SubProcessStrategy restoredStrategy = restored.strategyManager().getStrategy(SubProcessStrategy.class);

        assertNotNull(restoredStrategy);
        assertFalse(restoredStrategy.isResettable());
    }


    @Test
    void createTriggerNode() {
        IFlowNode node = NodeFactory.getInstance().createNode(NodeType.TRIGGER);
        assertNotNull(node);
        assertEquals(node.getType(), NodeType.TRIGGER.name());
    }
}

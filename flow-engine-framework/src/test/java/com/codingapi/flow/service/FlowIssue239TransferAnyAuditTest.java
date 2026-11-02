package com.codingapi.flow.service;

import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.builder.ActionBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowProcessNodeRequest;
import com.codingapi.flow.pojo.response.ProcessNode;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.MultiOperatorAuditStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * issue #239 转办场景测试。
 *
 * <p>流程设计：A(开始) -> B(审批，审批人 b1/b2，任意审核) -> C(审批，审批人 c1) -> D(结束)。
 *
 * <p>场景复现：
 * <ol>
 *     <li>流程到达 B 节点，b1、b2 同时收到待办；</li>
 *     <li>b1 不直接办理，而是把待办转办给 b3；</li>
 *     <li>b3 审批通过。</li>
 * </ol>
 *
 * <p>期望：b3 通过即代表 B 节点通过（或签语义），b2 的同节点待办应被自动办结，
 * 流程只进入 C 节点，c1 是唯一可审批的人，B 节点不再展示为审批中。
 *
 * <p>当前缺陷：转办生成的 b3 记录继承了 b1 记录作为 fromId，导致 b3 通过时
 * {@code findCurrentNodeRecords(fromId, nodeId)} 只查到自己，B 节点被判定为已完成，
 * 但 b2 的待办（另一条 fromId）不会被自动办结，同时流程又生成了 C 节点待办，
 * 出现 B、C 两个节点同时可审批的状态。
 */
class FlowIssue239TransferAnyAuditTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    @Test
    void transferredOperatorPassShouldFinishAnyAuditNodeAndAutoDoneSibling() {
        // given：A-B-C-D，B 为任意审核（b1/b2），C 的审批人为 c1
        User a = new User(1, "a");
        User b1 = new User(2, "b1");
        User b2 = new User(3, "b2");
        User c1 = new User(4, "c1");
        User b3 = new User(5, "b3");
        registerUsers(a, b1, b2, c1, b3);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[2,3]");
        ApprovalNode cNode = approvalNode("C审批", "[4]");
        EndNode endNode = EndNode.builder().build();
        Workflow workflow = saveWorkflow(a, startNode, bNode, cNode, endNode);
        Map<String, Object> data = data();

        // 发起并提交，流程到达 B 节点：b1、b2 各一条待办
        startAndSubmit(workflow, startNode, a, data);
        FlowRecord b1Todo = todoOf(b1);
        FlowRecord b2Todo = todoOf(b2);
        assertEquals(bNode.getId(), b1Todo.getNodeId());
        assertEquals(bNode.getId(), b2Todo.getNodeId());

        // when 1：b1 将待办转办给 b3
        transfer(b1Todo, bNode, b1, b3, data);

        // then 1：b1 已办，b3 收到 B 节点待办，b2 的待办仍在
        assertNoTodo(b1);
        assertEquals(bNode.getId(), todoOf(b3).getNodeId());
        assertEquals(bNode.getId(), todoOf(b2).getNodeId());

        // when 2：b3 审批通过
        pass(todoOf(b3), bNode, b3, data);

        // then 2：或签节点通过后，b2 的同节点待办应自动办结
        assertNoTodo(b2);

        // then 3：流程只进入 C 节点，c1 有且仅有一条待办
        List<FlowRecord> c1Todos = factory.flowRecordRepository.findTodoByOperator(c1.getUserId());
        assertEquals(1, c1Todos.size(), "流程应只生成一条 C 节点待办");
        assertEquals(cNode.getId(), c1Todos.get(0).getNodeId());

        // then 4：流程中不应残留任何 B 节点待办
        List<FlowRecord> records = processRecords(b1Todo);
        List<FlowRecord> bRecords = recordsOfNode(records, bNode);
        assertEquals(3, bRecords.size(), "B 节点应保留 b1、b2、b3 三条记录");
        assertEquals(0, bRecords.stream().filter(FlowRecord::isTodo).count(),
                "B 节点所有记录都应为已办，不应残留待办");

        // then 5：B 节点已结束（不得再展示为审批中），C 节点为审批中
        List<ProcessNode> nodeList = processNodes(c1Todos.get(0), a, data);
        List<ProcessNode> bViews = nodeViews(nodeList, bNode.getId());
        assertTrue(bViews.stream().noneMatch(node -> node.getApproveState() == ProcessNode.ApproveState.PROCESSING),
                "B 节点不应同时展示为审批中");
        ProcessNode cView = currentNodeView(nodeList, cNode.getId());
        assertEquals(ProcessNode.ApproveState.PROCESSING, cView.getApproveState(),
                "C 节点应为审批中");
    }

    // ==================== 流程装配 ====================

    private void registerUsers(User... users) {
        for (User user : users) {
            factory.userGateway.save(user);
        }
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);
    }

    private FlowForm form() {
        return FlowFormBuilder.builder()
                .name("issue239")
                .code("issue239")
                .addField("name", "name", DataType.STRING)
                .build();
    }

    private Map<String, Object> data() {
        return Map.of("name", "lorne");
    }

    private StartNode startNode() {
        return StartNode.builder()
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();
    }

    private ApprovalNode approvalNode(String name, String operatorIds) {
        return ApprovalNode.builder()
                .name(name)
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(
                                        "def run(request){return " + operatorIds + "}").getKey()))
                        .addStrategy(new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0))
                        .build())
                .build();
    }

    private Workflow saveWorkflow(User creator, StartNode start, ApprovalNode b, ApprovalNode c, EndNode end) {
        Workflow workflow = WorkflowBuilder.builder()
                .title("issue239")
                .code("issue239")
                .createdOperator(creator)
                .form(form())
                .addNode(start)
                .addNode(b)
                .addNode(c)
                .addNode(end)
                .build();
        factory.workflowService.saveWorkflow(workflow);
        return workflow;
    }

    // ==================== 操作与断言 ====================

    private void startAndSubmit(Workflow workflow, StartNode startNode, User user, Map<String, Object> data) {
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startNode.actionManager().getFirstAction().id());
        createRequest.setOperatorId(user.getUserId());
        factory.flowService.create(createRequest);

        pass(todoOf(user), startNode, user, data);
    }

    private void pass(FlowRecord record, IFlowNode node, User operator, Map<String, Object> data) {
        flowAction(record, action(node, ActionType.PASS), operator, data, "同意", null);
    }

    private void transfer(FlowRecord record, IFlowNode node, User operator,
                          User target, Map<String, Object> data) {
        flowAction(record, action(node, ActionType.TRANSFER), operator, data, "转办",
                List.of(target.getUserId()));
    }

    private IFlowAction action(IFlowNode node, ActionType type) {
        return node.actionManager().getActionByType(type.name());
    }

    private void flowAction(FlowRecord record, IFlowAction action, User operator,
                            Map<String, Object> data, String advice, List<Long> forwardOperatorIds) {
        FlowActionRequest request = new FlowActionRequest();
        request.setFormData(data);
        request.setRecordId(record.getId());
        FlowAdviceBody adviceBody = new FlowAdviceBody(action.id(), advice, operator.getUserId());
        adviceBody.setForwardOperatorIds(forwardOperatorIds);
        request.setAdvice(adviceBody);
        factory.flowService.action(request);
    }

    private FlowRecord todoOf(User user) {
        List<FlowRecord> list = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, list.size(), user.getName() + " 应有且仅有一条待办");
        return list.get(0);
    }

    private void assertNoTodo(User user) {
        assertEquals(0, factory.flowRecordRepository.findTodoByOperator(user.getUserId()).size(),
                user.getName() + " 不应有待办");
    }

    private List<FlowRecord> processRecords(FlowRecord anyRecord) {
        return factory.flowRecordRepository.findProcessRecords(anyRecord.getProcessId());
    }

    private List<FlowRecord> recordsOfNode(List<FlowRecord> records, ApprovalNode node) {
        return records.stream().filter(record -> record.getNodeId().equals(node.getId())).toList();
    }

    private List<ProcessNode> nodeViews(List<ProcessNode> nodeList, String nodeId) {
        return nodeList.stream()
                .filter(node -> nodeId.equals(node.getNodeId()))
                .toList();
    }

    private ProcessNode currentNodeView(List<ProcessNode> nodeList, String nodeId) {
        return nodeList.stream()
                .filter(node -> nodeId.equals(node.getNodeId()))
                .filter(node -> !node.isHistory())
                .findFirst()
                .orElseThrow(() -> new AssertionError("流程视图中缺少节点：" + nodeId));
    }

    private List<ProcessNode> processNodes(FlowRecord record, User viewer, Map<String, Object> data) {
        return factory.flowService.processNodes(
                new FlowProcessNodeRequest(record.getId(), viewer.getUserId(), data));
    }
}
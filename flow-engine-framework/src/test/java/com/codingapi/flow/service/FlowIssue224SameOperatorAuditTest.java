package com.codingapi.flow.service;

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
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.MultiOperatorAuditStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.SameOperatorAuditStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * issue #224：相同人员提交审批
 *
 * <p>流程：A(开始) -> B(审批) -> C(审批) -> D(结束)，发起人 a，C 节点审批人 c。
 *
 * <p>审批节点可配置 {@link SameOperatorAuditStrategy}（提交人与审批人一致时的处理方式）：
 * <ul>
 *     <li>AUTO_PASS —— 提交人与审批人为同一人时，自动通过该节点（无需本人审批）；</li>
 *     <li>MANUAL_PASS —— 即使提交人与审批人相同，仍需手动审批。</li>
 * </ul>
 *
 * <p>场景一：B 节点审批人仅为 a（与提交人一致）+ 相同人员自动审批，a 提交后 B 节点自动通过
 * （a 无待办，但保留一条自动通过的已办记录，issue #226），直达 C 节点由 c 审批。
 *
 * <p>场景二：B 节点审批人为 a、b（提交人 a 位列首位）+ 相同人员自动审批 + 依次审批，
 * a 提交后应自动跳过 a 本人，B 节点直接转交 b 审批。
 *
 * <p>控制组：B 节点审批人仅为 a + 相同人员手动审批，a 提交后仍需 a 本人审批 B 节点。
 */
class FlowIssue224SameOperatorAuditTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    // 人员：a=1 发起人，b=2 B 节点第二审批人，c=3 C 节点审批人
    private static final long A = 1L;
    private static final long B = 2L;
    private static final long C = 3L;

    // ==================== 基础工具方法 ====================

    private void registerUsers(User... users) {
        for (User user : users) {
            factory.userGateway.save(user);
        }
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);
    }

    private FlowForm form() {
        return FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .build();
    }

    private Map<String, Object> data() {
        return Map.of("name", "lorne", "days", 1);
    }

    private StartNode startNode() {
        return StartNode.builder().build();
    }

    /**
     * 构建审批节点
     *
     * @param name              节点名称
     * @param operatorIds       审批人脚本返回值，如 "[1]" / "[1,2]"
     * @param sameOperatorType  相同人员审批策略（AUTO_PASS 自动通过 / MANUAL_PASS 手动审批）
     * @param auditStrategy     多人审批策略，单审批人时传 null
     */
    private ApprovalNode approvalNode(String name, String operatorIds,
                                      SameOperatorAuditStrategy.Type sameOperatorType,
                                      MultiOperatorAuditStrategy auditStrategy) {
        NodeStrategyBuilder strategyBuilder = NodeStrategyBuilder.builder()
                .addStrategy(new SameOperatorAuditStrategy(sameOperatorType))
                .addStrategy(new OperatorLoadStrategy(
                        FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return " + operatorIds + "}").getKey()));
        if (auditStrategy != null) {
            strategyBuilder.addStrategy(auditStrategy);
        }
        return ApprovalNode.builder()
                .name(name)
                .strategies(strategyBuilder.build())
                .build();
    }

    private Workflow saveWorkflow(User creator, IFlowNode... nodes) {
        WorkflowBuilder builder = WorkflowBuilder.builder()
                .title("审批流程")
                .code("leave")
                .createdOperator(creator)
                .form(form());
        for (IFlowNode node : nodes) {
            builder.addNode(node);
        }
        Workflow workflow = builder.build();
        factory.workflowService.saveWorkflow(workflow);
        return workflow;
    }

    /**
     * 发起人提交开始节点（直接通过）
     */
    private void submitStart(Workflow workflow, StartNode startNode, User user, Map<String, Object> data) {
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startNode.actionManager().getActions().get(0).id());
        createRequest.setOperatorId(user.getUserId());
        factory.flowService.create(createRequest);

        FlowRecord todo = todoOf(user);
        pass(todo, startNode, user, data);
    }

    private void pass(FlowRecord record, IFlowNode node, User operator, Map<String, Object> data) {
        FlowActionRequest request = new FlowActionRequest();
        request.setFormData(data);
        request.setRecordId(record.getId());
        request.setAdvice(new FlowAdviceBody(node.actionManager().getActions().get(0).id(), "同意", operator.getUserId()));
        factory.flowService.action(request);
    }

    private FlowRecord todoOf(User user) {
        List<FlowRecord> list = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, list.size(), user.getName() + " 应有且仅有一条待办");
        return list.get(0);
    }

    private List<FlowRecord> todosOf(User user) {
        return factory.flowRecordRepository.findTodoByOperator(user.getUserId());
    }

    private void assertNoTodo(User user) {
        assertEquals(0, factory.flowRecordRepository.findTodoByOperator(user.getUserId()).size(),
                user.getName() + " 不应有待办");
    }

    private List<FlowRecord> processRecords(FlowRecord anyRecord) {
        return factory.flowRecordRepository.findProcessRecords(anyRecord.getProcessId());
    }

    // ==================== 场景一：单审批人 = 提交人，自动通过（保留已办记录，issue #226） ====================

    /**
     * 场景一：B 节点审批人仅为 a（与发起人一致）+ 相同人员自动审批（AUTO_PASS）。
     * <p>a 提交后 B 节点自动通过：a 无待办，但 B 节点保留一条无审批动作的自动通过已办记录（issue #226），
     * 直接流转到 C 节点由 c 审批。
     */
    @Test
    void autoPassShouldKeepDoneRecordAndSkipToDoWhenOnlyApproverIsInitiator() {
        User a = new User(A, "a");
        User c = new User(C, "c");
        registerUsers(a, c);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[1]",
                SameOperatorAuditStrategy.Type.AUTO_PASS, null);
        ApprovalNode cNode = approvalNode("C审批", "[3]",
                SameOperatorAuditStrategy.Type.AUTO_PASS, null);
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(a, startNode, bNode, cNode, endNode);
        submitStart(workflow, startNode, a, data());

        // a 本人不应收到 B 节点待办（相同人员自动通过）
        assertNoTodo(a);

        // 直接流转到 C 节点：c 收到待办，且待办节点为 C 审批节点
        FlowRecord cTodo = todoOf(c);
        assertEquals(cNode.getId(), cTodo.getNodeId(), "a 提交后应直达 C 节点，待办应落在 C 审批节点");

        // 流程记录：开始 + B 自动通过 + C 共 3 条（issue #226：自动通过保留已办记录留痕）
        List<FlowRecord> records = processRecords(cTodo);
        assertEquals(3, records.size(), "B 节点自动通过，流程记录应含 开始、B、C 三条");
        List<FlowRecord> bRecords = records.stream()
                .filter(r -> bNode.getId().equals(r.getNodeId()))
                .toList();
        assertEquals(1, bRecords.size(), "B 节点应保留一条自动通过的流程记录");
        assertTrue(bRecords.get(0).isDone(), "B 节点自动通过记录应为已办状态");
        assertTrue(bRecords.get(0).isAutoDone(), "B 节点记录应为无审批动作的自动办结（autoSkip）");

        // c 审批后流程正常结束
        pass(cTodo, cNode, c, data());
        records = processRecords(cTodo);
        assertEquals(3, records.size());
        assertTrue(records.stream().allMatch(FlowRecord::isFinish), "流程结束后全部记录应为完成状态");
        assertNoTodo(c);
    }

    // ==================== 场景二：多人依次审批 + 提交人位列首位，自动跳过提交人 ====================

    /**
     * 场景二：B 节点审批人为 a、b（提交人 a 位列首位）+ 相同人员自动审批（AUTO_PASS）
     * + 依次审批（SEQUENCE）。
     * <p>a 提交后应自动跳过 a 本人，B 节点直接转交 b 审批（b 收到待办、a 无待办）；
     * b 审批后流转到 C 节点由 c 审批。
     */
    @Test
    void autoPassWithSequenceShouldSkipInitiatorAndTransferToNextApprover() {
        User a = new User(A, "a");
        User b = new User(B, "b");
        User c = new User(C, "c");
        registerUsers(a, b, c);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[1,2]",
                SameOperatorAuditStrategy.Type.AUTO_PASS,
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.SEQUENCE, 0));
        ApprovalNode cNode = approvalNode("C审批", "[3]",
                SameOperatorAuditStrategy.Type.AUTO_PASS, null);
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(a, startNode, bNode, cNode, endNode);
        submitStart(workflow, startNode, a, data());

        // 提交人 a 被自动跳过：a 无待办、c 尚未收到待办
        assertNoTodo(a);
        assertNoTodo(c);

        // B 节点直接转交 b 审批：b 收到且仅收到一条待办，落在 B 审批节点
        List<FlowRecord> bTodos = todosOf(b);
        assertEquals(1, bTodos.size(), "B 节点应仅 b 一人收到待办");
        assertEquals(bNode.getId(), bTodos.get(0).getNodeId(), "b 的待办应落在 B 审批节点");

        // 流程记录：开始 + b（B 节点仅一条记录，a 的记录被跳过）共 2 条
        List<FlowRecord> records = processRecords(bTodos.get(0));
        assertEquals(2, records.size(), "B 节点仅产生 b 一条记录，a 的记录被自动跳过");
        assertEquals(1, records.stream().filter(r -> bNode.getId().equals(r.getNodeId())).count());

        // b 审批后流转到 C 节点：c 收到待办
        pass(bTodos.get(0), bNode, b, data());
        FlowRecord cTodo = todoOf(c);
        assertEquals(cNode.getId(), cTodo.getNodeId(), "b 审批后应流转到 C 审批节点");

        // c 审批后流程正常结束
        pass(cTodo, cNode, c, data());
        records = processRecords(cTodo);
        assertEquals(3, records.size());
        assertTrue(records.stream().allMatch(FlowRecord::isFinish), "流程结束后全部记录应为完成状态");
        assertNoTodo(b);
        assertNoTodo(c);
    }

    // ==================== 控制组：手动审批（MANUAL_PASS）时不跳过提交人 ====================

    /**
     * 控制组：B 节点审批人仅为 a + 相同人员手动审批（MANUAL_PASS）。
     * <p>即使提交人 a 与审批人一致，仍需 a 本人审批 B 节点，不能跳过。
     */
    @Test
    void manualPassShouldNotSkipInitiator() {
        User a = new User(A, "a");
        User c = new User(C, "c");
        registerUsers(a, c);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[1]",
                SameOperatorAuditStrategy.Type.MANUAL_PASS, null);
        ApprovalNode cNode = approvalNode("C审批", "[3]",
                SameOperatorAuditStrategy.Type.MANUAL_PASS, null);
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(a, startNode, bNode, cNode, endNode);
        submitStart(workflow, startNode, a, data());

        // a 仍需审批 B 节点，c 未收到待办
        FlowRecord aTodo = todoOf(a);
        assertEquals(bNode.getId(), aTodo.getNodeId(), "手动审批下 a 应收到 B 节点待办");
        assertNoTodo(c);

        // a 审批 B 节点后流转到 C 节点
        pass(aTodo, bNode, a, data());
        FlowRecord cTodo = todoOf(c);
        assertEquals(cNode.getId(), cTodo.getNodeId(), "a 审批 B 节点后应流转到 C 审批节点");

        // c 审批后流程结束
        pass(cTodo, cNode, c, data());
        assertNoTodo(a);
        assertNoTodo(c);
    }
}

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
 * issue #224：相同人员自动审批 × 四种多人审批模式
 *
 * <p>流程：A(开始) -> B(多人审批) -> C(c1 审批) -> D(结束)，发起人 a 提交，B 节点审批人 a、b、c。
 *
 * <p>验证：B 节点配置 {@link SameOperatorAuditStrategy.Type#AUTO_PASS}（提交人与审批人一致自动通过）
 * 时，发起人 a 在四种多人审批模式下的行为：
 * <ul>
 *     <li>SEQUENCE（依次审批）—— a 被自动跳过，剩余审批人依次办理；</li>
 *     <li>MERGE（会签，比例 1.0）—— a 被自动跳过，剩余审批人全部审批通过后节点完成；</li>
 *     <li>ANY（任意审批，或签）—— a 被自动跳过，剩余审批人任一办理即通过；</li>
 *     <li>RANDOM_ONE（任意一人）—— a 被自动跳过，剩余审批人中随机一人办理。</li>
 * </ul>
 *
 * <p>RANDOM_ONE 存在随机性：多轮循环创建流程，每轮断言「a 无待办、b/c 中恰有一人收到待办」，
 * 并统计被选中人员，最后断言 b、c 均被选中过（随机确实在两名候选人间切换）。
 */
class FlowIssue224SameOperatorAuditMultiModeTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    // 人员：a=1 发起人/提交人，b=2 / c=3 B 节点其余审批人，c1=4 C 节点审批人
    private static final long A = 1L;
    private static final long B = 2L;
    private static final long C = 3L;
    private static final long C1 = 4L;

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
     * 构建审批节点：审批人脚本 + 相同人员自动审批 + 多人审批模式
     *
     * @param name            节点名称
     * @param operatorIds     审批人脚本返回值，如 "[1,2,3]"
     * @param sameOperator    相同人员审批策略（AUTO_PASS 自动通过）
     * @param auditStrategy   多人审批模式（SEQUENCE / MERGE / ANY / RANDOM_ONE）
     */
    private ApprovalNode approvalNode(String name, String operatorIds,
                                      SameOperatorAuditStrategy sameOperator,
                                      MultiOperatorAuditStrategy auditStrategy) {
        NodeStrategyBuilder strategyBuilder = NodeStrategyBuilder.builder()
                .addStrategy(sameOperator)
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

    /**
     * 保存流程（多轮循环时传唯一 code，避免版本冲突）
     */
    private Workflow saveWorkflow(User creator, String code, IFlowNode... nodes) {
        WorkflowBuilder builder = WorkflowBuilder.builder()
                .title("审批流程")
                .code(code)
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

    private List<FlowRecord> recordsOfNode(List<FlowRecord> records, IFlowNode node) {
        return records.stream().filter(r -> r.getNodeId().equals(node.getId())).toList();
    }

    // ==================== 场景装配：A - B(四人审批) - C(c1) - D ====================

    /**
     * 构建标准流程：发起人 a、B 节点审批人 a/b/c（配置指定多人模式 + 自动通过）、C 节点 c1 审批。
     * <p>startNode 必须由调用方创建并复用（工作流内节点与提交用的节点为同一实例，
     * 动作 id 才一致——不同实例的动作 id 不同，create 按 id 查不到动作）。
     */
    private Workflow buildWorkflow(String code, User a, StartNode startNode,
                                   ApprovalNode bNode, ApprovalNode cNode, EndNode endNode) {
        return saveWorkflow(a, code, startNode, bNode, cNode, endNode);
    }

    private ApprovalNode bNode(String mode) {
        MultiOperatorAuditStrategy.Type type = switch (mode) {
            case "SEQUENCE" -> MultiOperatorAuditStrategy.Type.SEQUENCE;
            case "MERGE" -> MultiOperatorAuditStrategy.Type.MERGE;
            case "ANY" -> MultiOperatorAuditStrategy.Type.ANY;
            case "RANDOM_ONE" -> MultiOperatorAuditStrategy.Type.RANDOM_ONE;
            default -> throw new IllegalArgumentException("未知模式: " + mode);
        };
        return approvalNode("B审批", "[1,2,3]",
                new SameOperatorAuditStrategy(SameOperatorAuditStrategy.Type.AUTO_PASS),
                new MultiOperatorAuditStrategy(type, type == MultiOperatorAuditStrategy.Type.MERGE ? 1.0f : 0));
    }

    private ApprovalNode cNode() {
        return approvalNode("C审批", "[4]",
                new SameOperatorAuditStrategy(SameOperatorAuditStrategy.Type.AUTO_PASS), null);
    }

    // ==================== 依次审批（SEQUENCE）====================

    /**
     * SEQUENCE：B 节点审批人 a、b、c 依次审批 + 自动通过。
     * <p>a 作为提交人被自动跳过；b 收到待办（c 记录隐藏），b 审批后激活 c，c 审批后到 C 节点。
     */
    @Test
    void autoPassWithSequenceShouldSkipInitiatorAndKeepRemainingOrder() {
        User a = new User(A, "a");
        User b = new User(B, "b");
        User c = new User(C, "c");
        User c1 = new User(C1, "c1");
        registerUsers(a, b, c, c1);

        ApprovalNode bNode = bNode("SEQUENCE");
        ApprovalNode cNode = cNode();
        EndNode endNode = EndNode.builder().build();

        StartNode startNode = startNode();
        Workflow workflow = buildWorkflow("sequence", a, startNode, bNode, cNode, endNode);
        submitStart(workflow, startNode, a, data());

        // a 作为提交人被自动跳过：a 无待办；b 收到待办（依次首位），c 记录隐藏
        assertNoTodo(a);
        FlowRecord bTodo = todoOf(b);
        assertNoTodo(c);
        assertEquals(bNode.getId(), bTodo.getNodeId(), "b 的待办应落在 B 审批节点");

        // B 节点仅产生 b 一条记录（a 被跳过、c 隐藏），可见记录为 开始 + b 共 2 条
        List<FlowRecord> records = processRecords(bTodo);
        assertEquals(2, records.size(), "依次审批下 a 被自动跳过，B 节点应仅 b 一条可见记录");
        assertEquals(1, recordsOfNode(records, bNode).size());

        // b 审批 -> 激活 c：c 收到待办，流程未到 C
        pass(bTodo, bNode, b, data());
        FlowRecord cTodo = todoOf(c);
        assertNoTodo(b);
        assertNoTodo(c1);

        // c 审批 -> 到 C 节点：c1 收到待办
        pass(cTodo, bNode, c, data());
        FlowRecord c1Todo = todoOf(c1);
        assertEquals(cNode.getId(), c1Todo.getNodeId(), "c 审批后应流转到 C 审批节点");

        // B 节点 a、b、c 中仅 b、c 产生记录（a 被跳过），两条均为已办
        records = processRecords(c1Todo);
        assertEquals(2, recordsOfNode(records, bNode).size(), "B 节点应产生 b、c 两条记录，a 被跳过");
        assertEquals(2, recordsOfNode(records, bNode).stream().filter(FlowRecord::isDone).count());

        // c1 审批 -> 流程结束
        pass(c1Todo, cNode, c1, data());
        records = processRecords(c1Todo);
        assertEquals(4, records.size());
        assertTrue(records.stream().allMatch(FlowRecord::isFinish), "流程结束后全部记录应为完成状态");
        assertNoTodo(a);
        assertNoTodo(b);
        assertNoTodo(c);
        assertNoTodo(c1);
    }

    // ==================== 会签（MERGE，比例 1.0）====================

    /**
     * MERGE：B 节点审批人 a、b、c 会签（比例 1.0）+ 自动通过。
     * <p>a 作为提交人被自动跳过；b、c 均需审批通过（会签比例 1/2、2/2 分步达成），全部通过后到 C 节点。
     */
    @Test
    void autoPassWithMergeShouldSkipInitiatorAndRequireRemainingAllPass() {
        User a = new User(A, "a");
        User b = new User(B, "b");
        User c = new User(C, "c");
        User c1 = new User(C1, "c1");
        registerUsers(a, b, c, c1);

        ApprovalNode bNode = bNode("MERGE");
        ApprovalNode cNode = cNode();
        EndNode endNode = EndNode.builder().build();

        StartNode startNode = startNode();
        Workflow workflow = buildWorkflow("merge", a, startNode, bNode, cNode, endNode);
        submitStart(workflow, startNode, a, data());

        // a 被自动跳过；b、c 同时收到待办（会签）
        assertNoTodo(a);
        FlowRecord bTodo = todoOf(b);
        FlowRecord cTodo = todoOf(c);
        assertNoTodo(c1);

        // B 节点产生 b、c 两条记录（a 被跳过）
        List<FlowRecord> records = processRecords(bTodo);
        assertEquals(3, records.size(), "应为 开始 + b、c 两条会签记录 共 3 条");
        assertEquals(2, recordsOfNode(records, bNode).size(), "B 节点应产生 b、c 两条记录，a 被跳过");

        // b 审批 -> 会签比例 1/2 未达成，流程不向下流转，c 仍待办
        pass(bTodo, bNode, b, data());
        assertNoTodo(c1);
        cTodo = todoOf(c);
        records = processRecords(cTodo);
        assertEquals(1, records.stream().filter(FlowRecord::isTodo).count(), "仅 c 一条待办");

        // c 审批 -> 会签 2/2 达成，到 C 节点
        pass(cTodo, bNode, c, data());
        FlowRecord c1Todo = todoOf(c1);
        assertEquals(cNode.getId(), c1Todo.getNodeId(), "会签全部通过后应流转到 C 审批节点");

        records = processRecords(c1Todo);
        assertEquals(2, recordsOfNode(records, bNode).stream().filter(FlowRecord::isDone).count(),
                "B 节点 b、c 两条记录均应为已办");

        // c1 审批 -> 流程结束
        pass(c1Todo, cNode, c1, data());
        records = processRecords(c1Todo);
        assertEquals(4, records.size());
        assertTrue(records.stream().allMatch(FlowRecord::isFinish), "流程结束后全部记录应为完成状态");
        assertNoTodo(a);
        assertNoTodo(b);
        assertNoTodo(c);
        assertNoTodo(c1);
    }

    // ==================== 任意审批（ANY，或签）====================

    /**
     * ANY：B 节点审批人 a、b、c 或签 + 自动通过。
     * <p>a 作为提交人被自动跳过；b、c 任一办理即通过，另一人自动置为已办，流程到 C 节点。
     */
    @Test
    void autoPassWithAnyShouldSkipInitiatorAndPassOnFirstRemainingApproval() {
        User a = new User(A, "a");
        User b = new User(B, "b");
        User c = new User(C, "c");
        User c1 = new User(C1, "c1");
        registerUsers(a, b, c, c1);

        ApprovalNode bNode = bNode("ANY");
        ApprovalNode cNode = cNode();
        EndNode endNode = EndNode.builder().build();

        StartNode startNode = startNode();
        Workflow workflow = buildWorkflow("any", a, startNode, bNode, cNode, endNode);
        submitStart(workflow, startNode, a, data());

        // a 被自动跳过；b、c 同时收到待办（或签）
        assertNoTodo(a);
        FlowRecord bTodo = todoOf(b);
        todoOf(c);
        assertNoTodo(c1);

        // B 节点产生 b、c 两条记录（a 被跳过）
        List<FlowRecord> records = processRecords(bTodo);
        assertEquals(3, records.size(), "应为 开始 + b、c 两条或签记录 共 3 条");
        assertEquals(2, recordsOfNode(records, bNode).size(), "B 节点应产生 b、c 两条记录，a 被跳过");

        // b 办理（任一剩余审批人）-> c 自动置为已办，流程到 C 节点
        pass(bTodo, bNode, b, data());
        assertNoTodo(c);
        FlowRecord c1Todo = todoOf(c1);
        assertEquals(cNode.getId(), c1Todo.getNodeId(), "或签任一人办理后应流转到 C 审批节点");

        records = processRecords(c1Todo);
        assertEquals(2, recordsOfNode(records, bNode).stream().filter(FlowRecord::isDone).count(),
                "或签模式下 b、c 两条记录均应为已办");

        // c1 审批 -> 流程结束
        pass(c1Todo, cNode, c1, data());
        records = processRecords(c1Todo);
        assertEquals(4, records.size());
        assertTrue(records.stream().allMatch(FlowRecord::isFinish), "流程结束后全部记录应为完成状态");
        assertNoTodo(a);
        assertNoTodo(b);
        assertNoTodo(c);
        assertNoTodo(c1);
    }

    // ==================== 任意一人（RANDOM_ONE，多轮随机断言）====================

    /**
     * RANDOM_ONE：B 节点审批人 a、b、c 随机一人 + 自动通过。
     * <p>a 作为提交人被自动跳过；b、c 中随机一人收到待办，该人办理后到 C 节点。
     * <p>随机性验证：多轮创建流程，每轮断言「a 无待办、b/c 中恰有一人收到待办」，
     * 统计被选中人次，最后断言 b、c 均被选中过。
     */
    @Test
    void autoPassWithRandomOneShouldSkipInitiatorAndPickOneRandomly() {
        User a = new User(A, "a");
        User b = new User(B, "b");
        User c = new User(C, "c");
        User c1 = new User(C1, "c1");
        registerUsers(a, b, c, c1);

        int rounds = 12;
        int bChosen = 0;
        int cChosen = 0;

        for (int i = 0; i < rounds; i++) {
            ApprovalNode bNode = bNode("RANDOM_ONE");
            ApprovalNode cNode = cNode();
            EndNode endNode = EndNode.builder().build();
            StartNode startNode = startNode();
            Workflow workflow = buildWorkflow("random_" + i, a, startNode, bNode, cNode, endNode);
            submitStart(workflow, startNode, a, data());

            // a 作为提交人被自动跳过：a 无待办
            assertNoTodo(a);

            // b、c 中恰有一人收到待办，落在 B 节点
            List<FlowRecord> bTodos = todosOf(b);
            List<FlowRecord> cTodos = todosOf(c);
            assertEquals(1, bTodos.size() + cTodos.size(),
                    "第 " + i + " 轮：随机一人模式应仅一名审批人收到待办");

            boolean bChosenThisRound = bTodos.size() == 1;
            User chosen = bChosenThisRound ? b : c;
            FlowRecord chosenTodo = bChosenThisRound ? bTodos.get(0) : cTodos.get(0);
            assertEquals(bNode.getId(), chosenTodo.getNodeId(), "第 " + i + " 轮：待办应落在 B 审批节点");
            if (bChosenThisRound) {
                bChosen++;
            } else {
                cChosen++;
            }

            // B 节点仅一条记录（a 被跳过、随机一人）
            List<FlowRecord> records = processRecords(chosenTodo);
            assertEquals(2, records.size(), "第 " + i + " 轮：随机一人模式 B 节点应仅一条记录");
            assertEquals(1, recordsOfNode(records, bNode).size());

            // 选中的审批人办理 -> 到 C 节点
            pass(chosenTodo, bNode, chosen, data());
            FlowRecord c1Todo = todoOf(c1);
            assertEquals(cNode.getId(), c1Todo.getNodeId(), "第 " + i + " 轮：办理后应流转到 C 审批节点");

            // c1 审批 -> 流程结束
            pass(c1Todo, cNode, c1, data());
            records = processRecords(c1Todo);
            assertEquals(3, records.size());
            assertTrue(records.stream().allMatch(FlowRecord::isFinish), "第 " + i + " 轮：流程结束后全部记录应为完成状态");
            assertNoTodo(a);
            assertNoTodo(c1);
        }

        // 随机性验证：多轮中 b、c 均被选中过（随机确实在两名候选人间切换，而非固定一人）
        assertTrue(bChosen > 0, "多轮随机中 b 应至少被选中一次，实际 b=" + bChosen + ", c=" + cChosen);
        assertTrue(cChosen > 0, "多轮随机中 c 应至少被选中一次，实际 b=" + bChosen + ", c=" + cChosen);
    }
}

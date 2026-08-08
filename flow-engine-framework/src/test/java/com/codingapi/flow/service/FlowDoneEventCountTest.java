package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.builder.ActionBuilder;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.event.FlowRecordDoneEvent;
import com.codingapi.flow.event.FlowRecordFinishEvent;
import com.codingapi.flow.event.FlowRecordTodoEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.MultiOperatorAuditStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import com.codingapi.springboot.framework.event.DomainEvent;
import com.codingapi.springboot.framework.event.SpringEventInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 流程 Done 事件数量验证
 *
 * <p>流程设计：A(开始) -> B(任意审批/或签，3 人 b1/b2/b3) -> C(任意审批/或签，2 人 c1/c2) -> D(结束)。
 *
 * <p>验证目标：分别统计 B 节点、C 节点审批通过时，通过 {@code EventPusher} 推送的
 * {@link FlowRecordDoneEvent}（流程结束/记录办结事件）数量，并附带统计 Todo / Finish 事件作为对照。
 *
 * <p>事件捕获机制：引擎通过 {@code EventPusher.push} 推送事件，底层经 Spring
 * {@code ApplicationContext.publishEvent} 分发（无 Spring 上下文时事件被丢弃）。
 * 本测试注入一个 mock 的 {@code ApplicationContext}，在 {@code publishEvent} 处截获
 * {@link DomainEvent} 并记录其包装的 {@link IFlowEvent}。
 */
class FlowDoneEventCountTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();
    private final List<com.codingapi.springboot.framework.event.IEvent> capturedEvents = new ArrayList<>();

    @AfterEach
    void resetEventContext() throws Exception {
        // 还原 DomainEventContext 的 Spring 上下文，避免影响其他测试（无上下文时事件再次被丢弃）
        Class<?> clazz = Class.forName("com.codingapi.springboot.framework.event.DomainEventContext");
        java.lang.reflect.Method getInstance = clazz.getDeclaredMethod("getInstance");
        getInstance.setAccessible(true);
        Object instance = getInstance.invoke(null);
        Field field = clazz.getDeclaredField("context");
        field.setAccessible(true);
        field.set(instance, null);
    }

    /**
     * 注入 mock ApplicationContext，从 publishEvent 处截获引擎推送的全部事件
     */
    private void initEventCapture() throws Exception {
        ApplicationContext mockContext = Mockito.mock(ApplicationContext.class);
        Mockito.doAnswer(invocation -> {
            DomainEvent domainEvent = invocation.getArgument(0);
            capturedEvents.add(domainEvent.getEvent());
            return null;
        }).when(mockContext).publishEvent(Mockito.any(ApplicationEvent.class));
        new SpringEventInitializer(mockContext).afterPropertiesSet();
    }

    /**
     * 统计 fromIndex 之后捕获到的指定类型事件数量
     */
    private long countSince(int fromIndex, Class<? extends IFlowEvent> type) {
        return capturedEvents.subList(fromIndex, capturedEvents.size()).stream()
                .filter(type::isInstance)
                .count();
    }

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
                .addField("请假事由", "reason", DataType.STRING)
                .build();
    }

    private Map<String, Object> data(int days) {
        return Map.of("name", "lorne", "days", days, "reason", "leave");
    }

    private FormFieldPermissionStrategy perm() {
        return new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                .addPermission("leave", "name", PermissionType.WRITE)
                .addPermission("leave", "days", PermissionType.WRITE)
                .addPermission("leave", "reason", PermissionType.WRITE)
                .build());
    }

    private StartNode startNode() {
        return StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(perm())
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(com.codingapi.flow.action.actions.CustomAction.defaultAction())
                        .build())
                .build();
    }

    private ApprovalNode approvalNode(String name, String operatorIds, MultiOperatorAuditStrategy auditStrategy) {
        NodeStrategyBuilder strategyBuilder = NodeStrategyBuilder.builder()
                .addStrategy(perm())
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

    private void startAndSubmit(Workflow workflow, StartNode startNode, User user, Map<String, Object> data) {
        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(user.getUserId());
        factory.flowService.create(createRequest);

        FlowRecord todo = todoOf(user);
        pass(todo, startNode, user, data);
    }

    private void pass(FlowRecord record, IFlowNode node, User operator, Map<String, Object> data) {
        pass(record, node, operator, data, null);
    }

    private void pass(FlowRecord record, IFlowNode node, User operator, Map<String, Object> data, String signKey) {
        IFlowAction action = node.actionManager().getActions().get(0);
        FlowActionRequest request = new FlowActionRequest();
        request.setFormData(data);
        request.setRecordId(record.getId());
        FlowAdviceBody advice = new FlowAdviceBody(action.id(), "同意", operator.getUserId());
        advice.setSignKey(signKey);
        request.setAdvice(advice);
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

    // ==================== 场景：B 节点（或签 3 人）通过 ====================

    /**
     * B 节点（任意审批/或签，b1/b2/b3）由 b1 审批通过时，产生的 Done 事件数量。
     * <p>期望：3 个 Done 事件 —— b1 实际办理 1 个 + b2、b3 或签自动办结各 1 个。
     * 同时流程流转到 C 节点，产生 c1/c2 两个 Todo 事件；流程未结束，无 Finish 事件。
     */
    @Test
    void doneEventCountWhenAnyNodeBPassed() throws Exception {
        initEventCapture();
        User user = new User(1, "user");
        User b1 = new User(2, "b1");
        User b2 = new User(3, "b2");
        User b3 = new User(5, "b3");
        User c1 = new User(4, "c1");
        User c2 = new User(8, "c2");
        registerUsers(user, b1, b2, b3, c1, c2);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[2,3,5]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0));
        ApprovalNode cNode = approvalNode("C审批", "[4,8]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0));
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(user, startNode, bNode, cNode, endNode);
        Map<String, Object> data = data(1);
        startAndSubmit(workflow, startNode, user, data);

        // B 节点三人均收到待办
        FlowRecord b1Todo = todoOf(b1);
        todoOf(b2);
        todoOf(b3);

        // 记录 B 通过前的捕获快照，仅统计本次动作产生的事件
        int snapshot = capturedEvents.size();
        pass(b1Todo, bNode, b1, data);

        long done = countSince(snapshot, FlowRecordDoneEvent.class);
        long todo = countSince(snapshot, FlowRecordTodoEvent.class);
        long finish = countSince(snapshot, FlowRecordFinishEvent.class);

        // 核心断言：Done 事件数量
        assertEquals(3, done, "B 节点（或签3人）通过应产生 3 个 Done 事件（b1 实际办理 + b2/b3 或签自动办结）");

        // 对照：C 节点两人收到待办（Todo 事件），流程未结束（无 Finish 事件）
        assertEquals(2, todo, "B 通过后应产生 2 个 Todo 事件（c1/c2 待办）");
        assertEquals(0, finish, "B 通过后流程尚未结束，不应有 Finish 事件");

        // 记录状态：6 条记录（开始、b1/b2/b3、c1/c2），其中开始与 B 节点三人已办、C 两人待办
        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(b1Todo.getProcessId());
        assertEquals(6, records.size(), "应对应 开始、b1/b2/b3、c1/c2 共 6 条记录");
        assertEquals(4, records.stream().filter(FlowRecord::isDone).count(), "开始、b1/b2/b3 四条应为已办");
        assertEquals(2, records.stream().filter(FlowRecord::isTodo).count(), "c1/c2 应为待办");
        assertNoTodo(b2);
        assertNoTodo(b3);
    }

    // ==================== 场景：C 节点（或签 2 人）通过 ====================

    /**
     * C 节点（任意审批/或签，c1/c2）由 c1 审批通过时，产生的 Done 事件数量。
     * <p>期望：2 个 Done 事件 —— c1 实际办理 1 个 + c2 或签自动办结 1 个；
     * 流程到达结束节点，产生 1 个 Finish 事件。
     */
    @Test
    void doneEventCountWhenAnyNodeCPassed() throws Exception {
        initEventCapture();
        User user = new User(1, "user");
        User b1 = new User(2, "b1");
        User b2 = new User(3, "b2");
        User b3 = new User(5, "b3");
        User c1 = new User(4, "c1");
        User c2 = new User(8, "c2");
        registerUsers(user, b1, b2, b3, c1, c2);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[2,3,5]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0));
        ApprovalNode cNode = approvalNode("C审批", "[4,8]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0));
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(user, startNode, bNode, cNode, endNode);
        Map<String, Object> data = data(1);
        startAndSubmit(workflow, startNode, user, data);

        // 先走完 B 节点（b1 或签通过），流程到 C 节点
        FlowRecord b1Todo = todoOf(b1);
        pass(b1Todo, bNode, b1, data);
        FlowRecord c1Todo = todoOf(c1);
        todoOf(c2);

        // 记录 C 通过前的捕获快照，仅统计本次动作产生的事件
        int snapshot = capturedEvents.size();
        pass(c1Todo, cNode, c1, data);

        long done = countSince(snapshot, FlowRecordDoneEvent.class);
        long todo = countSince(snapshot, FlowRecordTodoEvent.class);
        long finish = countSince(snapshot, FlowRecordFinishEvent.class);

        // 核心断言：Done 事件数量
        assertEquals(2, done, "C 节点（或签2人）通过应产生 2 个 Done 事件（c1 实际办理 + c2 或签自动办结）");

        // 对照：流程到达结束节点，产生 1 个 Finish 事件
        assertEquals(1, finish, "C 通过后到达结束节点，应产生 1 个 Finish 事件");

        // 拦截验证：结束节点虚拟记录的 Todo 事件不应推送给业务下游
        assertEquals(0, todo, "C 通过后不应推送结束节点虚拟记录的 Todo 事件给业务下游");

        // 记录状态：6 条记录（开始、b1/b2/b3、c1/c2）全部完成，无待办
        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(c1Todo.getProcessId());
        assertEquals(6, records.size(), "应对应 开始、b1/b2/b3、c1/c2 共 6 条记录");
        assertEquals(6, records.stream().filter(FlowRecord::isFinish).count(), "流程结束后全部记录应为完成状态");
        assertNoTodo(c1);
        assertNoTodo(c2);
    }

    // ==================== 定位 issue #194：何种配置会产生观察到的现象 ====================

    /**
     * 定位 #194 问题一（"B节点出现的Done的事件是2个"）：或签节点人数为 2 人时，
     * 一人通过恰好产生 2 个 Done 事件（1 实际办理 + 1 自动办结）。
     * <p>即：B 为 2 人或签时 Done=2；B 为 3 人或签时 Done=3。事件数与节点审批人数一致。
     */
    @Test
    void doneEventCountWhenAnyNodeBHasTwoOperators() throws Exception {
        initEventCapture();
        User user = new User(1, "user");
        User b1 = new User(2, "b1");
        User b2 = new User(3, "b2");
        User c1 = new User(4, "c1");
        User c2 = new User(8, "c2");
        registerUsers(user, b1, b2, c1, c2);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[2,3]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0));
        ApprovalNode cNode = approvalNode("C审批", "[4,8]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0));
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(user, startNode, bNode, cNode, endNode);
        Map<String, Object> data = data(1);
        startAndSubmit(workflow, startNode, user, data);

        FlowRecord b1Todo = todoOf(b1);
        todoOf(b2);

        int snapshot = capturedEvents.size();
        pass(b1Todo, bNode, b1, data);

        assertEquals(2, countSince(snapshot, FlowRecordDoneEvent.class),
                "B 节点为 2 人或签时，一人通过产生 2 个 Done 事件（b1 实际办理 + b2 自动办结）");
    }

    /**
     * 定位 #194 问题二（"C走完时流程完成事件没有抛出，且 C 有一条记录未完成"）：
     * 当 C 为并签（MERGE，比例 1.0）而非或签时，c1 单独办理节点未完成，
     * c2 仍为待办、流程不向下流转、不抛 Finish 事件。
     * <p>即：只有 c1 完成、c2 未完成 + 无完成事件，对应的是 C 并签（需全部审批）
     * 的配置表现，而非或签（任一人通过即结束）。
     */
    @Test
    void finishEventWhenNodeCIsMergeRequiresAll() throws Exception {
        initEventCapture();
        User user = new User(1, "user");
        User b1 = new User(2, "b1");
        User b2 = new User(3, "b2");
        User c1 = new User(4, "c1");
        User c2 = new User(8, "c2");
        registerUsers(user, b1, b2, c1, c2);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[2,3]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0));
        // C 为并签（比例 1.0）：需 c1、c2 全部审批才能完成
        ApprovalNode cNode = approvalNode("C审批", "[4,8]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.MERGE, 1.0f));
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(user, startNode, bNode, cNode, endNode);
        Map<String, Object> data = data(1);
        startAndSubmit(workflow, startNode, user, data);

        // 走完 B 节点，流程到 C
        FlowRecord b1Todo = todoOf(b1);
        pass(b1Todo, bNode, b1, data);
        FlowRecord c1Todo = todoOf(c1);
        todoOf(c2);

        // c1 单独办理 -> 并签比例 1/2 未达成，流程不完成（复现 #194 观察：无完成事件、C 一条记录未完成）
        int snapshot = capturedEvents.size();
        pass(c1Todo, cNode, c1, data);

        assertEquals(0, countSince(snapshot, FlowRecordFinishEvent.class),
                "C 并签仅 c1 一人办理时，不应抛 Finish 事件");
        assertEquals(1, countSince(snapshot, FlowRecordDoneEvent.class),
                "c1 办理产生 1 个 Done 事件，c2 并签未完成不会自动办结");
        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(c1Todo.getProcessId());
        assertEquals(1, records.stream().filter(FlowRecord::isTodo).count(), "c2 仍应待办（C 一条记录未完成）");

        // c2 办理 -> 并签完成，流程结束，抛 Finish 事件
        FlowRecord c2Todo = todoOf(c2);
        snapshot = capturedEvents.size();
        pass(c2Todo, cNode, c2, data);
        assertEquals(1, countSince(snapshot, FlowRecordFinishEvent.class),
                "c2 办理后并签完成，应抛 1 个 Finish 事件");
    }

    // ==================== 场景：autoDone 记录不应携带历史签名/审批意见 ====================

    /**
     * 或签节点自动办结（autoDone）时，记录不应携带上一审批人遗留的签名 signKey 与审批意见。
     * <p>复现缺陷：新记录生成时 {@code FlowRecord} 构造函数从会话 advice 继承 signKey，
     * 而 {@code newRecord()} 只清空 advice 未清空 signKey，导致：
     * <ul>
     *     <li>B 节点 b1 带签名通过后，C 节点新生成的 c1/c2 待办记录携带 b1 的签名；</li>
     *     <li>c1 带签名通过后，c2 被自动办结（autoDone），仍携带 b1 的签名与审批意见。</li>
     * </ul>
     * <p>期望：与审批意见一致，新记录与自动办结记录均不应携带任何签名。
     */
    @Test
    void autoDoneRecordShouldNotCarryHistoricalSignKey() throws Exception {
        initEventCapture();
        User user = new User(1, "user");
        User b1 = new User(2, "b1");
        User b2 = new User(3, "b2");
        User b3 = new User(5, "b3");
        User c1 = new User(4, "c1");
        User c2 = new User(8, "c2");
        registerUsers(user, b1, b2, b3, c1, c2);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[2,3,5]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0));
        ApprovalNode cNode = approvalNode("C审批", "[4,8]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0));
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(user, startNode, bNode, cNode, endNode);
        Map<String, Object> data = data(1);
        startAndSubmit(workflow, startNode, user, data);

        // b1 带签名通过 B 节点
        FlowRecord b1Todo = todoOf(b1);
        pass(b1Todo, bNode, b1, data, "sig-b1");
        assertNoTodo(b2);
        assertNoTodo(b3);

        // C 节点新生成的待办记录不应携带 b1 的签名与审批意见
        FlowRecord c1Todo = todoOf(c1);
        FlowRecord c2Todo = todoOf(c2);
        assertNull(c1Todo.getSignKey(), "新生成的 C 待办不应携带上一审批人的签名");
        assertNull(c2Todo.getSignKey(), "新生成的 C 待办不应携带上一审批人的签名");
        assertNull(c1Todo.getAdvice(), "新生成的 C 待办不应携带审批意见");

        // c1 带签名通过 C 节点 -> c2 自动办结
        pass(c1Todo, cNode, c1, data, "sig-c1");

        // c2 为自动办结记录，不应携带任何签名与审批意见
        List<FlowRecord> processRecords = factory.flowRecordRepository.findProcessRecords(c1Todo.getProcessId());
        FlowRecord c2Record = processRecords.stream()
                .filter(r -> r.getNodeId().equals(cNode.getId()) && r.getCurrentOperatorId() == c2.getUserId())
                .findFirst().orElseThrow();
        assertTrue(c2Record.isAutoDone(), "c2 应为自动办结记录");
        assertNull(c2Record.getSignKey(), "自动办结记录不应携带历史审批签名 signKey");
        assertNull(c2Record.getAdvice(), "自动办结记录不应携带审批意见");
    }
}
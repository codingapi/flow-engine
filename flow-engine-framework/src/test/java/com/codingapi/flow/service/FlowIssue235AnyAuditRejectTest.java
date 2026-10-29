package com.codingapi.flow.service;

import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.action.actions.PassAction;
import com.codingapi.flow.action.actions.RejectAction;
import com.codingapi.flow.builder.ActionBuilder;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.event.FlowRecordFinishEvent;
import com.codingapi.flow.event.FlowRecordTodoEvent;
import com.codingapi.flow.exception.FlowStateException;
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
import com.codingapi.flow.pojo.request.FlowDetailRequest;
import com.codingapi.flow.pojo.request.FlowProcessNodeRequest;
import com.codingapi.flow.pojo.response.ProcessNode;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * issue #235 任意审批（或签）节点拒绝场景。
 *
 * <p>流程设计：A(开始) -> B(审批，审批人 b1) -> C(审批，审批人 c1/c2，任意审核) -> D(结束)。
 * C 节点拒绝动作退回开始节点。
 *
 * <p>当 c1 拒绝时，C 节点即告结束：
 * <ul>
 *     <li>c2 在同节点的待办应自动办结（视为已审核），不能继续审批；</li>
 *     <li>流程退回开始节点，发起人收到新的待办；</li>
 *     <li>流程节点视图中 C 节点不再处于审批中，也不会从遗留的 c2 待办错误预览后续的 D 节点。</li>
 * </ul>
 */
class FlowIssue235AnyAuditRejectTest {

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

    private void initEventCapture() throws Exception {
        ApplicationContext mockContext = Mockito.mock(ApplicationContext.class);
        Mockito.doAnswer(invocation -> {
            DomainEvent domainEvent = invocation.getArgument(0);
            capturedEvents.add(domainEvent.getEvent());
            return null;
        }).when(mockContext).publishEvent(Mockito.any(ApplicationEvent.class));
        new SpringEventInitializer(mockContext).afterPropertiesSet();
    }

    @Test
    void anyAuditRejectShouldAutoDoneSiblingTodoAndReturnToStart() {
        // given：A-B-C-D，B 的审批人为 b1，C 的审批人为 c1/c2，C 为任意审核，拒绝退回开始节点
        User a = new User(1, "a");
        User b1 = new User(2, "b1");
        User c1 = new User(3, "c1");
        User c2 = new User(4, "c2");
        registerUsers(a, b1, c1, c2);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[2]", null);
        RejectAction rejectAction = RejectAction.defaultAction();
        rejectAction.setScript(FlowGroovyScriptFactory
                .createActionRejectScript("def run(request){return request.getStartNode().getId()}")
                .getKey());
        ApprovalNode cNode = approvalNode("C审批", "[3,4]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0), rejectAction);
        EndNode endNode = EndNode.builder().build();
        Workflow workflow = saveWorkflow(a, startNode, bNode, cNode, endNode);
        Map<String, Object> data = data();

        // 1. 发起人提交，b1 审批通过，流程到达 C 节点
        startAndSubmit(workflow, startNode, a, data);
        pass(todoOf(b1), bNode, b1, data);
        FlowRecord c1Todo = todoOf(c1);
        FlowRecord c2Todo = todoOf(c2);
        assertEquals(cNode.getId(), c1Todo.getNodeId());
        assertEquals(cNode.getId(), c2Todo.getNodeId());

        // when：c1 拒绝
        reject(c1Todo, cNode, c1, data);

        // then 1：c2 的同节点待办自动办结，c1/c2 均无待办
        assertNoTodo(c1);
        assertNoTodo(c2);
        assertTrue(factory.flowRecordRepository.get(c2Todo.getId()).isDone(),
                "c1 拒绝后，c2 的同节点待办应自动置为已办");

        // then 2：流程退回开始节点，仅发起人存在一条新的待办
        List<FlowRecord> todoRecords = factory.flowRecordRepository.findTodoByOperator(a.getUserId());
        assertEquals(1, todoRecords.size(), "流程退回后发起人应收到一条待办");
        assertEquals(startNode.getId(), todoRecords.get(0).getNodeId());

        // then 3：C 节点两条记录均为已办，流程中不存在遗留的 C 节点待办
        List<FlowRecord> records = processRecords(c1Todo);
        List<FlowRecord> cRecords = recordsOfNode(records, cNode);
        assertEquals(2, cRecords.size(), "C 节点应保留 c1、c2 两条记录");
        assertEquals(2, cRecords.stream().filter(FlowRecord::isDone).count(),
                "C 节点两条记录都应为已办（c1 拒绝，c2 自动办结）");
        assertEquals(1, records.stream().filter(FlowRecord::isTodo).count(),
                "流程中仅应存在发起人的一条待办");

        // then 4：c2 不能再对该节点执行审批
        assertThrows(FlowStateException.class, () -> pass(c2Todo, cNode, c2, data),
                "c2 的记录已自动办结，不应再允许审批");

        // then 5：流程节点视图不再停留在 C 节点审批中
        List<ProcessNode> nodeList = processNodes(todoRecords.get(0), a, data);

        ProcessNode cNodeView = nodeList.stream()
                .filter(node -> cNode.getId().equals(node.getNodeId()))
                .filter(node -> !node.isHistory())
                .findFirst()
                .orElse(null);
        assertTrue(cNodeView == null || cNodeView.getApproveState() != ProcessNode.ApproveState.PROCESSING,
                "C 节点不应仍展示为审批中");

        ProcessNode cHistoryView = nodeList.stream()
                .filter(node -> cNode.getId().equals(node.getNodeId()))
                .filter(ProcessNode::isHistory)
                .findFirst()
                .orElseThrow();
        assertTrue(cHistoryView.getOperators().stream()
                        .noneMatch(operator -> ActionType.PASS.name().equals(operator.getActionType())),
                "C 节点历史中不应把拒绝误记为通过");
    }

    /**
     * 拒绝配置为「终止流程」时，或签节点的其他待办同样应自动办结，流程正常终止。
     */
    @Test
    void anyAuditRejectTerminateShouldCloseSiblingAndTerminateFlow() throws Exception {
        // given：A-B-C-D，C 为任意审核（c1/c2），拒绝动作终止流程
        User a = new User(1, "a");
        User b1 = new User(2, "b1");
        User c1 = new User(3, "c1");
        User c2 = new User(4, "c2");
        registerUsers(a, b1, c1, c2);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[2]", null);
        RejectAction rejectAction = RejectAction.defaultAction();
        rejectAction.setScript(FlowGroovyScriptFactory
                .createActionRejectScript("def run(request){return 'TERMINATE'}")
                .getKey());
        ApprovalNode cNode = approvalNode("C审批", "[3,4]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0), rejectAction);
        EndNode endNode = EndNode.builder().build();
        Workflow workflow = saveWorkflow(a, startNode, bNode, cNode, endNode);
        Map<String, Object> data = data();

        // 1. 流程到达 C 节点，c1、c2 同时收到待办
        startAndSubmit(workflow, startNode, a, data);
        pass(todoOf(b1), bNode, b1, data);
        FlowRecord c1Todo = todoOf(c1);
        FlowRecord c2Todo = todoOf(c2);

        // 捕获拒绝环节推送的事件，用于校验结束节点虚拟待办事件被拦截
        initEventCapture();
        capturedEvents.clear();

        // when：c1 拒绝并终止流程
        reject(c1Todo, cNode, c1, data);

        // then：c1、c2 均无待办，流程终止
        assertNoTodo(c1);
        assertNoTodo(c2);
        List<FlowRecord> records = processRecords(c1Todo);
        assertEquals(2, recordsOfNode(records, cNode).stream().filter(FlowRecord::isDone).count(),
                "C 节点两条记录都应为已办");
        assertTrue(records.stream().filter(FlowRecord::isTodo).findFirst().isEmpty(),
                "流程终止后不应遗留待办");
        assertTrue(records.stream()
                        .filter(record -> cNode.getId().equals(record.getNodeId()))
                        .allMatch(FlowRecord::isFinish),
                "流程终止后 C 节点记录应处于结束状态");

        // 结束节点的虚拟记录（operatorId=-1）不是真实业务待办，不应推送 Todo 事件，
        // 否则订阅方按操作人加载时会拿到空对象而出现空指针
        assertTrue(capturedEvents.stream()
                        .filter(FlowRecordTodoEvent.class::isInstance)
                        .map(FlowRecordTodoEvent.class::cast)
                        .allMatch(event -> event.getFlowRecord().isNotEndNode()),
                "结束节点虚拟记录不应推送待办事件");
        assertTrue(capturedEvents.stream().anyMatch(FlowRecordFinishEvent.class::isInstance),
                "流程终止后应推送流程结束事件");

        // 流程终止后查询流程视图应正常返回
        List<ProcessNode> nodeList = processNodes(records.get(records.size() - 1), a, data);
        assertTrue(nodeList.stream().anyMatch(node -> cNode.getId().equals(node.getNodeId())),
                "流程视图应包含 C 节点");

        // 以自动办结的 c2 记录为入口查询流程视图与详情，均不应报错
        processNodes(c2Todo, c2, data);
        factory.flowService.detail(new FlowDetailRequest(c2Todo.getId(), c2.getUserId()));
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
                .name("issue235")
                .code("issue235")
                .addField("name", "name", DataType.STRING)
                .addField("days", "days", DataType.INTEGER)
                .build();
    }

    private Map<String, Object> data() {
        return Map.of("name", "lorne", "days", 1);
    }

    private FormFieldPermissionStrategy perm() {
        return new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                .addPermission("issue235", "name", PermissionType.WRITE)
                .addPermission("issue235", "days", PermissionType.WRITE)
                .build());
    }

    private StartNode startNode() {
        return StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(perm())
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();
    }

    private ApprovalNode approvalNode(String name, String operatorIds, MultiOperatorAuditStrategy auditStrategy) {
        return approvalNode(name, operatorIds, auditStrategy, null);
    }

    private ApprovalNode approvalNode(String name, String operatorIds,
                                      MultiOperatorAuditStrategy auditStrategy,
                                      RejectAction rejectAction) {
        NodeStrategyBuilder strategyBuilder = NodeStrategyBuilder.builder()
                .addStrategy(perm())
                .addStrategy(new OperatorLoadStrategy(
                        FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return " + operatorIds + "}")
                                .getKey()));
        if (auditStrategy != null) {
            strategyBuilder.addStrategy(auditStrategy);
        }
        ActionBuilder actionBuilder = ActionBuilder.builder().addAction(PassAction.defaultAction());
        if (rejectAction != null) {
            actionBuilder.addAction(rejectAction);
        }
        return ApprovalNode.builder()
                .name(name)
                .strategies(strategyBuilder.build())
                .actions(actionBuilder.build())
                .build();
    }

    private Workflow saveWorkflow(User creator, IFlowNode... nodes) {
        WorkflowBuilder builder = WorkflowBuilder.builder()
                .title("issue235")
                .code("issue235")
                .createdOperator(creator)
                .form(form());
        for (IFlowNode node : nodes) {
            builder.addNode(node);
        }
        Workflow workflow = builder.build();
        factory.workflowService.saveWorkflow(workflow);
        return workflow;
    }

    // ==================== 操作与断言 ====================

    private void startAndSubmit(Workflow workflow, StartNode startNode, User user, Map<String, Object> data) {
        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(user.getUserId());
        factory.flowService.create(createRequest);

        pass(todoOf(user), startNode, user, data);
    }

    private void pass(FlowRecord record, IFlowNode node, User operator, Map<String, Object> data) {
        IFlowAction action = node.actionManager().getActions().stream()
                .filter(item -> ActionType.PASS.name().equals(item.type()))
                .findFirst().orElseThrow();
        flowAction(record, operator, action, data, "同意");
    }

    private void reject(FlowRecord record, IFlowNode node, User operator, Map<String, Object> data) {
        IFlowAction action = node.actionManager().getActions().stream()
                .filter(item -> ActionType.REJECT.name().equals(item.type()))
                .findFirst().orElseThrow();
        flowAction(record, operator, action, data, "拒绝");
    }

    private void flowAction(FlowRecord record, User operator, IFlowAction action,
                            Map<String, Object> data, String advice) {
        FlowActionRequest request = new FlowActionRequest();
        request.setFormData(data);
        request.setRecordId(record.getId());
        request.setAdvice(new FlowAdviceBody(action.id(), advice, operator.getUserId()));
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

    private List<FlowRecord> recordsOfNode(List<FlowRecord> records, IFlowNode node) {
        return records.stream().filter(record -> record.getNodeId().equals(node.getId())).toList();
    }

    private List<ProcessNode> processNodes(FlowRecord record, User viewer, Map<String, Object> data) {
        return factory.flowService.processNodes(
                new FlowProcessNodeRequest(record.getId(), viewer.getUserId(), data));
    }
}
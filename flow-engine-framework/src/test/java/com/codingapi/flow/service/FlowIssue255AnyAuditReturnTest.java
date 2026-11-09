package com.codingapi.flow.service;

import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.action.actions.PassAction;
import com.codingapi.flow.action.actions.ReturnAction;
import com.codingapi.flow.builder.ActionBuilder;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * issue #255 多人审批节点「退回」场景。
 *
 * <p>流程设计：A(开始) -> B(审批，b1/b2 或签) -> C(审批，c1) -> D(结束)。
 *
 * <p>流程停留在 B 节点时，b1 将流程退回到开始节点，B 节点即告结束：
 * <ul>
 *     <li>b2 在同节点的待办应自动办结（视为已审核），不能继续审批；</li>
 *     <li>发起人重新提交后，B 节点重新生成待办，b2 不应出现两条同流程待办。</li>
 * </ul>
 */
class FlowIssue255AnyAuditReturnTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    /**
     * 或签场景：b1 退回后，b2 的待办必须一起清理；重新提交后不得出现重复待办。
     */
    @Test
    void anyAuditReturnShouldAutoDoneSiblingTodoAndAvoidDuplicateTodoAfterResubmit() {
        // given：A-B-C-D，B 为任意审核（b1、b2），C 的审批人为 c1
        User a1 = new User(1, "a1");
        User b1 = new User(2, "b1");
        User b2 = new User(3, "b2");
        User c1 = new User(4, "c1");
        registerUsers(a1, b1, b2, c1);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[2,3]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0), true);
        ApprovalNode cNode = approvalNode("C审批", "[4]", null, false);
        Workflow workflow = saveWorkflow(a1, startNode, bNode, cNode, EndNode.builder().build());
        Map<String, Object> data = data();

        // 1. 发起人提交，B 节点 b1、b2 各收到一条待办
        startAndSubmit(workflow, startNode, a1, data);
        FlowRecord b1Todo = todoOf(b1, bNode);
        FlowRecord b2Todo = todoOf(b2, bNode);

        // when：b1 将流程退回到开始节点
        backToStart(b1Todo, bNode, b1, startNode, data);

        // then 1：b2 的同节点待办自动办结，b1、b2 均无待办
        assertNoTodo(b1);
        assertNoTodo(b2);
        FlowRecord b2AutoDone = factory.flowRecordRepository.get(b2Todo.getId());
        assertTrue(b2AutoDone.isDone(), "b1 退回后，b2 的同节点待办应自动置为已办");
        assertTrue(b2AutoDone.isAutoDone(), "b2 未发生实际审批，应标记为自动办结");

        // then 2：流程回到开始节点，仅发起人有一条待办
        List<FlowRecord> a1Todos = factory.flowRecordRepository.findTodoByOperator(a1.getUserId());
        assertEquals(1, a1Todos.size(), "流程退回后发起人应收到一条待办");
        assertEquals(startNode.getId(), a1Todos.get(0).getNodeId());

        // then 3：B 节点不再展示为审批中
        List<ProcessNode> nodeList = processNodes(a1Todos.get(0), a1, data);
        ProcessNode bView = nodeList.stream()
                .filter(node -> bNode.getId().equals(node.getNodeId()))
                .filter(node -> !node.isHistory())
                .findFirst()
                .orElse(null);
        assertTrue(bView == null || bView.getApproveState() != ProcessNode.ApproveState.PROCESSING,
                "退回后 B 节点不应仍展示为审批中");

        // 2. 发起人重新提交，流程再次到达 B 节点
        pass(todoOf(a1, startNode), startNode, a1, data);

        // then 4：b1、b2 各只有一条待办，不出现同流程重复待办
        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(b1.getUserId()).size(),
                "b1 不应出现同流程重复待办");
        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(b2.getUserId()).size(),
                "b2 不应出现同流程重复待办");

        List<FlowRecord> bRecords = recordsOfNode(processRecords(b2Todo), bNode);
        assertEquals(4, bRecords.size(), "B 节点应保留两轮共 4 条记录");
        assertEquals(2, bRecords.stream().filter(FlowRecord::isDone).count(),
                "上一轮的 b1、b2 记录都应为已办");
        assertEquals(2, bRecords.stream().filter(FlowRecord::isTodo).count(),
                "当前轮次应有 b1、b2 两条待办");

        // then 5：上一轮遗留的待办不能再被审批
        assertThrows(FlowStateException.class, () -> pass(b2Todo, bNode, b2, data),
                "b2 的旧记录已自动办结，不应再允许审批");

        // then 6：新一轮 b2 通过（或签）后流程正常到达 C 节点
        pass(todoOf(b2, bNode), bNode, b2, data);
        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(c1.getUserId()).size(),
                "或签通过后流程应流转到 C 节点");
    }

    /**
     * 并签场景：b2 退回后，同节点尚未办理的 b3 待办同样应自动办结，且已发生的真实审批记录不被改写。
     */
    @Test
    void mergeAuditReturnShouldAutoDoneSiblingTodos() {
        // given：A-B-D，B 为并签（b1、b2、b3，需全部通过）
        User a1 = new User(1, "a1");
        User b1 = new User(2, "b1");
        User b2 = new User(3, "b2");
        User b3 = new User(4, "b3");
        registerUsers(a1, b1, b2, b3);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[2,3,4]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.MERGE, 1.0f), true);
        Workflow workflow = saveWorkflow(a1, startNode, bNode, EndNode.builder().build());
        Map<String, Object> data = data();

        // 1. b1 审批通过，并签未达到比例，b2、b3 仍有待办
        startAndSubmit(workflow, startNode, a1, data);
        pass(todoOf(b1, bNode), bNode, b1, data);
        FlowRecord b3Todo = todoOf(b3, bNode);

        // when：b2 将流程退回到开始节点
        backToStart(todoOf(b2, bNode), bNode, b2, startNode, data);

        // then：b2、b3 均无待办，b3 的记录自动办结
        assertNoTodo(b2);
        assertNoTodo(b3);
        FlowRecord b3AutoDone = factory.flowRecordRepository.get(b3Todo.getId());
        assertTrue(b3AutoDone.isDone(), "b2 退回后，b3 的同节点待办应自动置为已办");
        assertTrue(b3AutoDone.isAutoDone(), "b3 未发生实际审批，应标记为自动办结");

        List<FlowRecord> bRecords = recordsOfNode(processRecords(b3Todo), bNode);
        assertTrue(bRecords.stream()
                        .filter(FlowRecord::isDone)
                        .anyMatch(record -> ActionType.PASS.name().equals(record.getActionType())),
                "b1 已发生的通过记录不应被改写");
        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(a1.getUserId()).size(),
                "流程退回后发起人应收到一条待办");
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
                .name("issue255")
                .code("issue255")
                .addField("name", "name", DataType.STRING)
                .addField("days", "days", DataType.INTEGER)
                .build();
    }

    private Map<String, Object> data() {
        return Map.of("name", "lorne", "days", 1);
    }

    private FormFieldPermissionStrategy perm() {
        return new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                .addPermission("issue255", "name", PermissionType.WRITE)
                .addPermission("issue255", "days", PermissionType.WRITE)
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

    private ApprovalNode approvalNode(String name, String operatorIds,
                                      MultiOperatorAuditStrategy auditStrategy, boolean withReturn) {
        NodeStrategyBuilder strategyBuilder = NodeStrategyBuilder.builder()
                .addStrategy(perm())
                .addStrategy(new OperatorLoadStrategy(
                        FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return " + operatorIds + "}")
                                .getKey()));
        if (auditStrategy != null) {
            strategyBuilder.addStrategy(auditStrategy);
        }
        ActionBuilder actionBuilder = ActionBuilder.builder().addAction(PassAction.defaultAction());
        if (withReturn) {
            actionBuilder.addAction(ReturnAction.defaultAction());
        }
        return ApprovalNode.builder()
                .name(name)
                .strategies(strategyBuilder.build())
                .actions(actionBuilder.build())
                .build();
    }

    private Workflow saveWorkflow(User creator, IFlowNode... nodes) {
        WorkflowBuilder builder = WorkflowBuilder.builder()
                .title("issue255")
                .code("issue255")
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

        pass(todoOf(user, startNode), startNode, user, data);
    }

    private void pass(FlowRecord record, IFlowNode node, User operator, Map<String, Object> data) {
        IFlowAction action = node.actionManager().getActions().stream()
                .filter(item -> ActionType.PASS.name().equals(item.type()))
                .findFirst().orElseThrow();
        flowAction(record, operator, action, data, "同意");
    }

    private void backToStart(FlowRecord record, IFlowNode node, User operator,
                             StartNode startNode, Map<String, Object> data) {
        IFlowAction action = node.actionManager().getActions().stream()
                .filter(item -> ActionType.RETURN.name().equals(item.type()))
                .findFirst().orElseThrow();
        FlowActionRequest request = new FlowActionRequest();
        request.setFormData(data);
        request.setRecordId(record.getId());
        FlowAdviceBody adviceBody = new FlowAdviceBody(action.id(), "退回", operator.getUserId());
        adviceBody.setBackNodeId(startNode.getId());
        request.setAdvice(adviceBody);
        factory.flowService.action(request);
    }

    private void flowAction(FlowRecord record, User operator, IFlowAction action,
                            Map<String, Object> data, String advice) {
        FlowActionRequest request = new FlowActionRequest();
        request.setFormData(data);
        request.setRecordId(record.getId());
        request.setAdvice(new FlowAdviceBody(action.id(), advice, operator.getUserId()));
        factory.flowService.action(request);
    }

    private FlowRecord todoOf(User user, IFlowNode node) {
        List<FlowRecord> list = factory.flowRecordRepository.findTodoByOperator(user.getUserId()).stream()
                .filter(record -> node.getId().equals(record.getNodeId()))
                .toList();
        assertEquals(1, list.size(), user.getName() + " 在 " + node.getName() + " 应有且仅有一条待办");
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

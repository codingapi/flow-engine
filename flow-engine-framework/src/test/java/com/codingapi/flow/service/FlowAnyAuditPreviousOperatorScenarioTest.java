package com.codingapi.flow.service;

import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.builder.ActionBuilder;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * issue #188 场景测试：上一节点为"任意人审核（或签）"时，下游节点取数与流程节点视图展示。
 *
 * <p>流程设计：A(发起) -> B(任意人审核 b1、b2、b3) -> C(审批人=findPreviousNodeOperatorIds) -> D(结束)。
 * <ul>
 *     <li>B 节点：脚本加载审批人 b1、b2、b3，多操作者策略为 ANY（或签，任一人办理即可）</li>
 *     <li>C 节点：脚本模式，通过 {@code request.findPreviousNodeOperatorIds()} 取上一节点审批人</li>
 * </ul>
 *
 * <p>场景事实：B 节点三位候选人 b1、b2、b3 均收到待办；b3 实际审批通过后，b1、b2 的待办
 * 被自动置为已办（autoDone，未发生审批动作），流程流转到 C 节点。
 *
 * <p>验证点（对应 issue 的两个 bug）：
 * <ol>
 *     <li>Bug1 —— C 节点取数：审批人应为 B 节点<b>实际审批人</b> b3，
 *     而非全体候选人 b1、b2、b3（未实际审批、被自动跳过的候选人不应被取到）</li>
 *     <li>Bug2 —— 视图展示：B 节点仍展示全部候选人（展示层不过滤），但在数据结构上
 *     标记未实际审批的候选人为"自动跳过"（autoSkip），而非由前端自行推断</li>
 * </ol>
 */
class FlowAnyAuditPreviousOperatorScenarioTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    private User user;
    private User b1;
    private User b2;
    private User b3;

    private StartNode startNode;
    private ApprovalNode bNode;
    private ApprovalNode cNode;
    private EndNode endNode;
    private Workflow workflow;

    private Map<String, Object> data;

    @BeforeEach
    void setUp() {
        user = new User(1, "user");
        b1 = new User(2, "b1");
        b2 = new User(3, "b2");
        b3 = new User(4, "b3");
        registerUsers(user, b1, b2, b3);

        FlowForm form = FlowFormBuilder.builder()
                .name("审批流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        // A：发起节点
        startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(perm())
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();

        // B：任意人审核（或签），候选人 b1、b2、b3
        bNode = ApprovalNode.builder()
                .name("B审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(perm())
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(
                                        "def run(request){return [2,3,4]}").getKey()))
                        .addStrategy(new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0))
                        .build())
                .build();

        // C：审批人取上一节点（B 节点）审批人
        cNode = ApprovalNode.builder()
                .name("C审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(perm())
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(cNodeOperatorScript()).getKey()))
                        .build())
                .build();

        // D：结束节点
        endNode = EndNode.builder().build();

        workflow = WorkflowBuilder.builder()
                .title("审批流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bNode)
                .addNode(cNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        data = Map.of("name", "lorne", "days", 1, "reason", "leave");
    }

    /**
     * C 节点审批人脚本：取上一节点（B 节点）的审批人员。
     * 取数逻辑由 {@code FlowSession#loadPreviousNodeOperatorIds()} 提供。
     */
    private String cNodeOperatorScript() {
        return """
                def run(request) {
                    return request.findPreviousNodeOperatorIds()
                }
                """;
    }

    // ==================== 基础工具方法 ====================

    private void registerUsers(User... users) {
        for (User u : users) {
            factory.userGateway.save(u);
        }
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);
    }

    private FormFieldPermissionStrategy perm() {
        return new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                .addPermission("leave", "name", PermissionType.WRITE)
                .addPermission("leave", "days", PermissionType.WRITE)
                .addPermission("leave", "reason", PermissionType.WRITE)
                .build());
    }

    /**
     * 创建并提交（通过）发起节点，流程流转到 B 节点。
     */
    private void startAndSubmit() {
        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(user.getUserId());
        factory.flowService.create(createRequest);

        FlowRecord todo = todoOf(user);
        pass(startNode, todo, user);
    }

    /**
     * 以节点第一个动作（通过）办理记录。
     */
    private void pass(IFlowNode node, FlowRecord record, User operator) {
        IFlowAction action = node.actionManager().getActions().get(0);
        FlowActionRequest request = new FlowActionRequest();
        request.setFormData(data);
        request.setRecordId(record.getId());
        request.setAdvice(new FlowAdviceBody(action.id(), "同意", operator.getUserId()));
        factory.flowService.action(request);
    }

    private FlowRecord todoOf(User operator) {
        List<FlowRecord> list = factory.flowRecordRepository.findTodoByOperator(operator.getUserId());
        assertEquals(1, list.size(), operator.getName() + " 应有且仅有一条待办");
        return list.get(0);
    }

    private void assertNoTodo(User... operators) {
        for (User operator : operators) {
            assertEquals(0, factory.flowRecordRepository.findTodoByOperator(operator.getUserId()).size(),
                    operator.getName() + " 不应有待办");
        }
    }

    /**
     * 查询流程节点视图（流程记录展示数据）。
     */
    private List<ProcessNode> processNodes(FlowRecord record, User viewer) {
        return factory.flowService.processNodes(new FlowProcessNodeRequest(record.getId(), viewer.getUserId(), data));
    }

    private ProcessNode nodeOf(List<ProcessNode> nodeList, IFlowNode node) {
        return nodeList.stream()
                .filter(n -> node.getId().equals(n.getNodeId()))
                .findFirst()
                .orElse(null);
    }

    private ProcessNode.FlowOperatorBody operatorOf(List<ProcessNode.FlowOperatorBody> operators, User operator) {
        return operators.stream()
                .filter(body -> body.getFlowOperator() != null
                        && body.getFlowOperator().getUserId() == operator.getUserId())
                .findFirst()
                .orElse(null);
    }

    // ==================== 测试用例 ====================

    /**
     * Bug1 验证：B 节点任意人审核（或签），b3 实际审批后，
     * C 节点审批人应为 B 节点实际审批人 b3，而非全体候选人 b1、b2、b3。
     */
    @Test
    void cNodeOperatorShouldBeActualApprover_whenBNodeAnyAudit() {
        // 发起人提交 -> B 节点，b1、b2、b3 均收到 B 节点待办
        startAndSubmit();
        assertEquals(bNode.getId(), todoOf(b1).getNodeId());
        assertEquals(bNode.getId(), todoOf(b2).getNodeId());
        FlowRecord b3Todo = todoOf(b3);
        assertEquals(bNode.getId(), b3Todo.getNodeId());

        // b3 实际审批通过 -> B 完成，b1、b2 待办自动跳过，流程流转到 C 节点
        pass(bNode, b3Todo, b3);
        assertNoTodo(b1, b2);

        // C 节点审批人 = 上一节点实际审批人 = b3：仅 b3 收到 C 节点待办
        List<FlowRecord> b3Todos = factory.flowRecordRepository.findTodoByOperator(b3.getUserId());
        assertEquals(1, b3Todos.size(), "b3 应为 C 节点唯一审批人（实际审批 B 节点的人）");
        assertEquals(cNode.getId(), b3Todos.get(0).getNodeId());
        // b1、b2 未实际审批 B 节点，不应被取数为 C 节点审批人
        assertNoTodo(b1, b2);
    }

    /**
     * Bug2 验证：B 节点任意人审核（或签）完成后，流程节点视图中
     * B 节点仍展示全部候选人 b1、b2、b3（展示层不过滤），
     * 但数据结构上标记未实际审批的候选人为"自动跳过"（autoSkip）。
     */
    @Test
    void processNodeViewShouldMarkAutoSkippedOperators_whenBNodeAnyAudit() {
        // 发起人提交 -> b3 审批 B 节点 -> 流程流转到 C 节点
        startAndSubmit();
        FlowRecord b3Todo = todoOf(b3);
        pass(bNode, b3Todo, b3);

        // 以 b3 的 C 节点待办视角查看流程节点视图
        FlowRecord cTodo = todoOf(b3);
        assertEquals(cNode.getId(), cTodo.getNodeId());
        List<ProcessNode> nodeList = processNodes(cTodo, b3);

        // B 节点视图：或签策略，展示全部三位候选人（不过滤）
        ProcessNode bNodeView = nodeOf(nodeList, bNode);
        assertNotNull(bNodeView);
        assertEquals(MultiOperatorAuditStrategy.Type.ANY, bNodeView.getApproveStrategy());
        List<ProcessNode.FlowOperatorBody> operators = bNodeView.getOperators();
        assertEquals(3, operators.size(), "或签场景应展示全部候选审批人，展示层不过滤");

        // b3：实际审批，有审批动作，非自动跳过
        ProcessNode.FlowOperatorBody b3Body = operatorOf(operators, b3);
        assertNotNull(b3Body);
        assertEquals(ActionType.PASS.name(), b3Body.getActionType());
        assertFalse(b3Body.isAutoSkip(), "实际审批人不应标记为自动跳过");

        // b1、b2：自动跳过，无审批动作，数据结构上标记为 autoSkip
        for (User skipped : List.of(b1, b2)) {
            ProcessNode.FlowOperatorBody body = operatorOf(operators, skipped);
            assertNotNull(body, skipped.getName() + " 应以候选审批人身份展示");
            assertNull(body.getActionType(), skipped.getName() + " 未实际审批，不应有审批动作");
            assertTrue(body.isAutoSkip(), skipped.getName() + " 应标记为自动跳过");
        }
    }
}
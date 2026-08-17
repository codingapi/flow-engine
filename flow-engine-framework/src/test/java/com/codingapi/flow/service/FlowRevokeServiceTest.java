package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.HandleNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowRevokeRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.RevokeStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 撤回服务场景测试
 * <p>
 * 覆盖 FlowRevokeService 的守卫分支与 REVOKE_NEXT(退回撤回下级) 逻辑：
 * <ul>
 *   <li>记录不存在 / 待办 / 已完成 / 操作人不匹配 / 节点不支持撤回</li>
 *   <li>REVOKE_NEXT：下级仍为待办时允许撤回，下级全部完成时禁止撤回</li>
 * </ul>
 */
class FlowRevokeServiceTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    private final User user = new User(1, "user");
    private final User boss = new User(2, "boss");
    private final User cfo = new User(3, "cfo");

    private static final Map<String, Object> DATA = Map.of("name", "lorne", "days", 1, "reason", "leave");

    @BeforeEach
    void setUp() {
        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(cfo);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);
    }

    /**
     * 记录不存在时撤回抛异常
     */
    @Test
    void revoke_recordNotFound_shouldThrow() {
        assertThrows(FlowNotFoundException.class,
                () -> factory.flowService.revoke(new FlowRevokeRequest(999999, user.getUserId())));
    }

    /**
     * 撤回一条仍处于待办状态的发起记录
     */
    @Test
    void revoke_todoRecord_shouldThrow() {
        Workflow workflow = workflowChain("revoke-todo", RevokeStrategy.Type.REVOKE_CURRENT, boss.getUserId());
        factory.workflowService.saveWorkflow(workflow);

        FlowRecord startTodo = createFlow(workflow, user.getUserId());

        // 发起后未流转，记录为待办，不允许撤回
        assertTrue(startTodo.isTodo());
        assertThrows(FlowStateException.class,
                () -> factory.flowService.revoke(new FlowRevokeRequest(startTodo.getId(), user.getUserId())));
    }

    /**
     * 撤回一条已结束(整个流程完成)的记录
     */
    @Test
    void revoke_finishedRecord_shouldThrow() {
        Workflow workflow = workflowChain("revoke-finish", RevokeStrategy.Type.REVOKE_CURRENT, boss.getUserId());
        factory.workflowService.saveWorkflow(workflow);

        FlowRecord startDone = submitStartToFirstApproval(workflow, user.getUserId());
        FlowRecord bossDone = approve(workflow, boss, startDone.getProcessId());

        // 流程已到终态，记录 isFinish，不允许撤回
        assertTrue(bossDone.isFinish());
        assertThrows(FlowStateException.class,
                () -> factory.flowService.revoke(new FlowRevokeRequest(bossDone.getId(), boss.getUserId())));
    }

    /**
     * 撤回时当前操作人与记录操作人不匹配
     */
    @Test
    void revoke_operatorNotMatch_shouldThrow() {
        Workflow workflow = workflowChain("revoke-operator", RevokeStrategy.Type.REVOKE_CURRENT, boss.getUserId());
        factory.workflowService.saveWorkflow(workflow);

        FlowRecord startDone = submitStartToFirstApproval(workflow, user.getUserId());

        // 操作人不是记录当前操作人(999)
        assertThrows(FlowStateException.class,
                () -> factory.flowService.revoke(new FlowRevokeRequest(startDone.getId(), 999)));
    }

    /**
     * 当前节点未配置 RevokeStrategy(或未启用) 时不允许撤回
     * <p>
     * HandleNode(办理节点) 默认策略不含 RevokeStrategy，故节点不支持撤回。
     * Handle 节点后追加审批节点以保证办理完成后流程仍处于运行中(RUNNING)，
     * 避免 isFinish 驱逐在 nodeNotSupportRevoke 之前抛出。
     */
    @Test
    void revoke_nodeNotSupportRevoke_shouldThrow() {
        Workflow workflow = handleWorkflow("revoke-node");
        factory.workflowService.saveWorkflow(workflow);

        FlowRecord startDone = submitStartToFirstApproval(workflow, user.getUserId());

        // 用户办理 HandleNode，产生已办记录(所在节点无撤回策略)；后继审批节点仍在等待，流程保持运行
        FlowRecord handleTodo = factory.flowRecordRepository.findTodoByOperator(user.getUserId()).stream()
                .filter(record -> record.getId() != startDone.getId())
                .findFirst().orElseThrow();
        approve(workflow, user, handleTodo.getId());
        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(boss.getUserId()).size());

        assertThrows(FlowStateException.class,
                () -> factory.flowService.revoke(new FlowRevokeRequest(handleTodo.getId(), user.getUserId())));
    }

    /**
     * REVOKE_NEXT：下级仍为待办时，撤回会连同下级待办一并撤销
     */
    @Test
    void revoke_nextType_whenNextTodo_shouldRevokeNextRecords() {
        Workflow workflow = workflowChain("revoke-next-todo", RevokeStrategy.Type.REVOKE_NEXT, boss.getUserId(), cfo.getUserId());
        factory.workflowService.saveWorkflow(workflow);

        FlowRecord startDone = submitStartToFirstApproval(workflow, user.getUserId());

        // 下级(boss)待办存在
        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(boss.getUserId()).size());

        // 撤回不应抛异常，且下级待办被撤销，发起人回到待办
        factory.flowService.revoke(new FlowRevokeRequest(startDone.getId(), user.getUserId()));

        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(user.getUserId()).size());
        assertEquals(0, factory.flowRecordRepository.findTodoByOperator(boss.getUserId()).size());
    }

    /**
     * REVOKE_NEXT：下级直接后继记录已全部完成时，不允许撤回
     */
    @Test
    void revoke_nextType_whenNextDone_shouldThrow() {
        Workflow workflow = workflowChain("revoke-next-done", RevokeStrategy.Type.REVOKE_NEXT, boss.getUserId(), cfo.getUserId());
        factory.workflowService.saveWorkflow(workflow);

        FlowRecord startDone = submitStartToFirstApproval(workflow, user.getUserId());

        // boss 完成 B 节点，仍有更下级的 cfo 待办，流程仍运行中
        approve(workflow, boss, startDone.getProcessId());
        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(cfo.getUserId()).size());

        // 发起节点的直接后继(boss 的 B 记录)已完成 -> 禁止撤回
        assertThrows(FlowStateException.class,
                () -> factory.flowService.revoke(new FlowRevokeRequest(startDone.getId(), user.getUserId())));
    }

    /**
     * 撤回参数校验：recordId/operatorId 非法时抛参数校验异常
     */
    @Test
    void revoke_invalidRequest_shouldThrow() {
        assertThrows(com.codingapi.flow.exception.FlowValidationException.class,
                () -> factory.flowService.revoke(new FlowRevokeRequest(0, user.getUserId())));
        assertThrows(com.codingapi.flow.exception.FlowValidationException.class,
                () -> factory.flowService.revoke(new FlowRevokeRequest(1, 0)));
    }

    // ------------------ 私有助手 ------------------

    /**
     * 链式审批流程：Start -> [审批节点...] -> End
     *
     * @param code              流程编码
     * @param revokeType        Start 节点撤回类型(REVOKE_CURRENT / REVOKE_NEXT)
     * @param approvalOperatorIds 各审批节点操作人
     */
    private Workflow workflowChain(String code, RevokeStrategy.Type revokeType, long... approvalOperatorIds) {
        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(fieldPermission(code))
                        .addStrategy(new RevokeStrategy(true, revokeType))
                        .build())
                .build();

        WorkflowBuilder builder = WorkflowBuilder.builder()
                .title("请假流程").code(code).createdOperator(user).form(leaveForm(code))
                .addNode(startNode);
        for (long operatorId : approvalOperatorIds) {
            builder.addNode(approvalNode(code, operatorId));
        }
        return builder.addNode(EndNode.builder().build()).build();
    }

    /**
     * 办理节点流程：Start -> Handle(操作人=user) -> 审批(boss) -> End，用于验证"节点不支持撤回"。
     * 使用办理节点定位用户已办记录所在节点(无 RevokeStrategy)；追加 boss 审批保持流程运行。
     */
    private Workflow handleWorkflow(String code) {
        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(fieldPermission(code))
                        .build())
                .build();
        HandleNode handleNode = HandleNode.builder()
                .name("办理")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(fieldPermission(code))
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [" + user.getUserId() + "]}").getKey()))
                        .build())
                .build();
        return WorkflowBuilder.builder()
                .title("办理流程").code(code).createdOperator(user).form(leaveForm(code))
                .addNode(startNode).addNode(handleNode).addNode(approvalNode(code, boss.getUserId())).addNode(EndNode.builder().build())
                .build();
    }

    private FormFieldPermissionStrategy fieldPermission(String code) {
        return new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                .addPermission(code, "name", PermissionType.WRITE)
                .addPermission(code, "days", PermissionType.WRITE)
                .addPermission(code, "reason", PermissionType.WRITE)
                .build());
    }

    private ApprovalNode approvalNode(String code, long operatorId) {
        return ApprovalNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(fieldPermission(code))
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [" + operatorId + "]}").getKey()))
                        .build())
                .build();
    }

    private FlowForm leaveForm(String code) {
        return FlowFormBuilder.builder()
                .name("请假流程").code(code)
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();
    }

    /**
     * 发起流程，返回发起的 StartNode 记录(待办)
     */
    private FlowRecord createFlow(Workflow workflow, long operatorId) {
        StartNode startNode = (StartNode) workflow.getStartNode();
        IFlowAction startAction = startNode.actionManager().getActions().get(0);
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(DATA);
        createRequest.setActionId(startAction.id());
        createRequest.setOperatorId(operatorId);
        long recordId = factory.flowService.create(createRequest);
        return factory.flowRecordRepository.get(recordId);
    }

    /**
     * 发起并流转到第一个审批节点，返回发起人的已办记录(StartNode 记录)
     */
    private FlowRecord submitStartToFirstApproval(Workflow workflow, long operatorId) {
        FlowRecord startTodo = createFlow(workflow, operatorId);
        StartNode startNode = (StartNode) workflow.getStartNode();
        IFlowAction startAction = startNode.actionManager().getActions().get(0);
        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(DATA);
        actionRequest.setRecordId(startTodo.getId());
        actionRequest.setAdvice(new FlowAdviceBody(startAction.id(), "同意", operatorId));
        factory.flowService.action(actionRequest);
        return startTodo;
    }

    /**
     * 审批某条待办记录(取节点第一个动作)
     */
    private void approve(Workflow workflow, User approver, long recordId) {
        FlowRecord record = factory.flowRecordRepository.get(recordId);
        IFlowAction action = workflow.getFlowNode(record.getNodeId()).actionManager().getActions().get(0);
        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(DATA);
        actionRequest.setRecordId(recordId);
        actionRequest.setAdvice(new FlowAdviceBody(action.id(), "同意", approver.getUserId()));
        factory.flowService.action(actionRequest);
    }

    /**
     * 审批由指定操作人待办中第一条属于该流程的记录，返回该已办记录
     */
    private FlowRecord approve(Workflow workflow, User approver, String processId) {
        FlowRecord todo = factory.flowRecordRepository.findTodoByOperator(approver.getUserId()).stream()
                .filter(record -> record.getProcessId().equals(processId))
                .findFirst().orElseThrow();
        approve(workflow, approver, todo.getId());
        return todo;
    }
}
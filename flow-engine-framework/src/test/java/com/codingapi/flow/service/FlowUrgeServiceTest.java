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
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowUrgeRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 催办服务场景测试
 * <p>
 * 覆盖 FlowUrgeService 的守卫分支与催办频率限制逻辑：
 * <ul>
 *   <li>记录不存在 / 待办 / 已完成 / 操作人不匹配</li>
 *   <li>催办间隔内重复催办提示频率限制异常</li>
 * </ul>
 */
class FlowUrgeServiceTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    private final User user = new User(1, "user");
    private final User boss = new User(2, "boss");

    private static final Map<String, Object> DATA = Map.of("name", "lorne", "days", 1, "reason", "leave");

    @BeforeEach
    void setUp() {
        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);
    }

    /**
     * 催办不存在的记录
     */
    @Test
    void urge_recordNotFound_shouldThrow() {
        assertThrows(FlowNotFoundException.class,
                () -> factory.flowService.urge(new FlowUrgeRequest(999999, user.getUserId())));
    }

    /**
     * 催办一条仍处于待办状态的发起记录
     */
    @Test
    void urge_todoRecord_shouldThrow() {
        Workflow workflow = workflowChain("urge-todo", boss.getUserId());
        factory.workflowService.saveWorkflow(workflow);

        FlowRecord startTodo = createFlow(workflow, user.getUserId());
        assertTrue(startTodo.isTodo());

        assertThrows(FlowStateException.class,
                () -> factory.flowService.urge(new FlowUrgeRequest(startTodo.getId(), user.getUserId())));
    }

    /**
     * 催办一条已结束(整个流程完成)的记录
     */
    @Test
    void urge_finishedRecord_shouldThrow() {
        Workflow workflow = workflowChain("urge-finish", boss.getUserId());
        factory.workflowService.saveWorkflow(workflow);

        FlowRecord startDone = submitStartToFirstApproval(workflow, user.getUserId());
        FlowRecord bossDone = approve(workflow, boss, startDone.getProcessId());

        assertTrue(bossDone.isFinish());
        assertThrows(FlowStateException.class,
                () -> factory.flowService.urge(new FlowUrgeRequest(bossDone.getId(), boss.getUserId())));
    }

    /**
     * 催办时当前操作人与记录操作人不匹配
     */
    @Test
    void urge_operatorNotMatch_shouldThrow() {
        Workflow workflow = workflowChain("urge-operator", boss.getUserId());
        factory.workflowService.saveWorkflow(workflow);

        FlowRecord startDone = submitStartToFirstApproval(workflow, user.getUserId());

        assertThrows(FlowStateException.class,
                () -> factory.flowService.urge(new FlowUrgeRequest(startDone.getId(), 999)));
    }

    /**
     * 催办参数校验：recordId/operatorId 非法时抛参数校验异常
     */
    @Test
    void urge_invalidRequest_shouldThrow() {
        assertThrows(com.codingapi.flow.exception.FlowValidationException.class,
                () -> factory.flowService.urge(new FlowUrgeRequest(0, user.getUserId())));
        assertThrows(com.codingapi.flow.exception.FlowValidationException.class,
                () -> factory.flowService.urge(new FlowUrgeRequest(1, 0)));
    }

    /**
     * 催办间隔内重复催办提示频率限制异常
     * <p>
     * 首次催办保存催办间隔，间隔内再次催办应被拒绝。
     * 工作流默认装配 InterfereStrategy(enable=true) 与 UrgeStrategy(interval=60s)，
     * isEnableUrge() 语义上基于默认策略生效，故第二次催办会进入频率校验。
     */
    @Test
    void urge_within_interval_shouldThrowLimit() {
        Workflow workflow = workflowChain("urge-limit", boss.getUserId());
        factory.workflowService.saveWorkflow(workflow);

        FlowRecord startDone = submitStartToFirstApproval(workflow, user.getUserId());

        FlowUrgeRequest request = new FlowUrgeRequest(startDone.getId(), user.getUserId());
        factory.flowService.urge(request); // 首次催办，保存催办间隔

        assertThrows(FlowStateException.class, () -> factory.flowService.urge(request));
    }

    // ------------------ 私有助手 ------------------

    private Workflow workflowChain(String code, long... approvalOperatorIds) {
        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(fieldPermission(code))
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

    private void approve(Workflow workflow, User approver, long recordId) {
        FlowRecord record = factory.flowRecordRepository.get(recordId);
        IFlowAction action = workflow.getFlowNode(record.getNodeId()).actionManager().getActions().get(0);
        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(DATA);
        actionRequest.setRecordId(recordId);
        actionRequest.setAdvice(new FlowAdviceBody(action.id(), "同意", approver.getUserId()));
        factory.flowService.action(actionRequest);
    }

    private FlowRecord approve(Workflow workflow, User approver, String processId) {
        List<FlowRecord> todoRecords = factory.flowRecordRepository.findTodoByOperator(approver.getUserId());
        FlowRecord todo = todoRecords.stream()
                .filter(record -> record.getProcessId().equals(processId))
                .findFirst().orElseThrow();
        approve(workflow, approver, todo.getId());
        return todo;
    }
}
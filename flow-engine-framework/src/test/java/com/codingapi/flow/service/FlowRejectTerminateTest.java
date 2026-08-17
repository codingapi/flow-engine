package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.PassAction;
import com.codingapi.flow.action.actions.RejectAction;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 拒绝(退回)动作场景测试
 * <p>
 * 覆盖 RejectAction 的 TERMINATE(终止流程)与其返回不存在的节点时抛出异常的路径：
 * <ul>
 *   <li>拒绝动作返回 "TERMINATE" -> 流程被终止(结束)</li>
 *   <li>拒绝动作返回不存在的节点 id -> currentNodeNotNull 异常</li>
 * </ul>
 */
class FlowRejectTerminateTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    private final User user = new User(1, "user");
    private final User boss = new User(2, "boss");

    private static final String CODE = "reject";
    private static final Map<String, Object> DATA = Map.of("name", "lorne", "days", 1, "reason", "leave");

    @BeforeEach
    void setUp() {
        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);
    }

    /**
     * 拒绝动作返回 "TERMINATE" 时，流程终止到结束节点
     */
    @Test
    void reject_terminate_shouldFinishProcess() {
        Workflow workflow = buildWorkflow("def run(request){return 'TERMINATE'}");
        factory.workflowService.saveWorkflow(workflow);

        FlowRecord startDone = submitStartToFirstApproval(workflow, user.getUserId());
        FlowRecord bossTodo = factory.flowRecordRepository.findTodoByOperator(boss.getUserId()).stream()
                .filter(record -> record.getProcessId().equals(startDone.getProcessId()))
                .findFirst().orElseThrow();

        // boss 拒绝 -> TERMINATE
        reject(workflow, boss, bossTodo.getId());

        // 流程终止：无剩余待办，全部记录进入终态
        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(startDone.getProcessId());
        assertEquals(0, factory.flowRecordRepository.findTodoByOperator(boss.getUserId()).size());
        assertTrue(records.stream().allMatch(FlowRecord::isFinish));
    }

    /**
     * 拒绝动作返回不存在的节点 id，无法生成有效记录 -> currentNodeNotNull 异常
     */
    @Test
    void reject_returnNonexistentNode_shouldThrow() {
        Workflow workflow = buildWorkflow("def run(request){return 'NOT_EXIST'}");
        factory.workflowService.saveWorkflow(workflow);

        FlowRecord startDone = submitStartToFirstApproval(workflow, user.getUserId());
        FlowRecord bossTodo = factory.flowRecordRepository.findTodoByOperator(boss.getUserId()).stream()
                .filter(record -> record.getProcessId().equals(startDone.getProcessId()))
                .findFirst().orElseThrow();

        assertThrows(FlowStateException.class, () -> reject(workflow, boss, bossTodo.getId()));
    }

    // ------------------ 私有助手 ------------------

    private Workflow buildWorkflow(String rejectScript) {
        RejectAction rejectAction = RejectAction.defaultAction();
        rejectAction.setScript(FlowGroovyScriptFactory.createActionRejectScript(rejectScript).getKey());

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder().addStrategy(fieldPermission()).build())
                .build();
        ApprovalNode bNode = ApprovalNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(fieldPermission())
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [" + boss.getUserId() + "]}").getKey()))
                        .build())
                .actions(List.of(PassAction.defaultAction(), rejectAction))
                .build();
        return WorkflowBuilder.builder()
                .title("拒绝流程").code(CODE).createdOperator(user).form(leaveForm())
                .addNode(startNode).addNode(bNode).addNode(EndNode.builder().build())
                .build();
    }

    private FormFieldPermissionStrategy fieldPermission() {
        return new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                .addPermission(CODE, "name", PermissionType.WRITE)
                .addPermission(CODE, "days", PermissionType.WRITE)
                .addPermission(CODE, "reason", PermissionType.WRITE)
                .build());
    }

    private FlowForm leaveForm() {
        return FlowFormBuilder.builder()
                .name("请假流程").code(CODE)
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();
    }

    private FlowRecord submitStartToFirstApproval(Workflow workflow, long operatorId) {
        StartNode startNode = (StartNode) workflow.getStartNode();
        IFlowAction startAction = startNode.actionManager().getActions().get(0);
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(DATA);
        createRequest.setActionId(startAction.id());
        createRequest.setOperatorId(operatorId);
        long recordId = factory.flowService.create(createRequest);
        FlowRecord startTodo = factory.flowRecordRepository.get(recordId);

        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(DATA);
        actionRequest.setRecordId(recordId);
        actionRequest.setAdvice(new FlowAdviceBody(startAction.id(), "同意", operatorId));
        factory.flowService.action(actionRequest);
        return startTodo;
    }

    private void reject(Workflow workflow, User approver, long recordId) {
        FlowRecord record = factory.flowRecordRepository.get(recordId);
        IFlowAction rejectAction = workflow.getFlowNode(record.getNodeId()).actionManager().getActions().stream()
                .filter(action -> action.type().equals("REJECT"))
                .findFirst().orElseThrow();
        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(DATA);
        actionRequest.setRecordId(recordId);
        actionRequest.setAdvice(new FlowAdviceBody(rejectAction.id(), "拒绝", approver.getUserId()));
        factory.flowService.action(actionRequest);
    }
}
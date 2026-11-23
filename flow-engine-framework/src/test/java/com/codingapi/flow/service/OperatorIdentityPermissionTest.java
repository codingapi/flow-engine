package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
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
import com.codingapi.flow.manager.WorkflowStrategyManager;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowDeleteRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.workflow.InterfereStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 审批判定的身份维度测试（issue #288 P3）。
 *
 * <p>同一人员的两个身份是**两个不同的参与人**：user1 以 department1 的身份不能办理
 * department2 的待办、不能撤销/删除对方身份的记录。身份为空时行为与历史完全一致。
 */
class OperatorIdentityPermissionTest {

    private MyFlowServiceFactory factory;
    private Workflow workflow;

    @BeforeEach
    void setUp() {
        User user1 = User.withIdentity(1, "user1", "department1");
        User boss = new User(2, "boss");
        factory = new MyFlowServiceFactory();
        factory.userGateway.save(user1);
        factory.userGateway.save(boss);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave-identity")
                .addField("请假人", "name", DataType.STRING)
                .build();

        FormFieldPermissionsBuilder permissions = FormFieldPermissionsBuilder.builder()
                .addPermission("leave-identity", "name", PermissionType.WRITE);

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(permissions.build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();

        ApprovalNode approvalNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(permissions.build()))
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();

        workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave-identity")
                .createdOperator(user1)
                .form(form)
                .addNode(startNode)
                .addNode(approvalNode)
                .addNode(endNode)
                .build();
        factory.workflowService.saveWorkflow(workflow);
    }

    private Map<String, Object> formData() {
        return Map.of("name", "lorne");
    }

    private List<IFlowAction> startActions() {
        return workflow.getStartNode().actionManager().getActions();
    }

    private long createFlow(String operatorIdentity) {
        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode(workflow.getCode());
        request.setFormData(formData());
        request.setActionId(startActions().get(0).id());
        request.setOperatorId(1);
        request.setOperatorIdentity(operatorIdentity);
        return factory.flowService.create(request);
    }

    private void action(long recordId, String operatorIdentity) {
        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(formData());
        actionRequest.setRecordId(recordId);
        FlowAdviceBody advice = new FlowAdviceBody(startActions().get(0).id(), "同意", 1);
        advice.setOperatorIdentity(operatorIdentity);
        actionRequest.setAdvice(advice);
        factory.flowService.action(actionRequest);
    }

    /**
     * 单元层面：同一 userId、不同身份必须判定为不匹配。
     */
    @Test
    void should_reject_same_user_with_other_identity_in_strategy_manager() {
        InterfereStrategy interfereStrategy = new InterfereStrategy();
        interfereStrategy.setEnable(false);
        WorkflowStrategyManager manager = new WorkflowStrategyManager(List.of(interfereStrategy));

        assertDoesNotThrow(() -> manager.verifyOperator(User.withIdentity(1, "user1", "department1"), 1, "department1"));
        assertThrows(FlowStateException.class,
                () -> manager.verifyOperator(User.withIdentity(1, "user1", "department1"), 1, "department2"));
        // 身份为空（默认身份）时与历史行为一致
        assertDoesNotThrow(() -> manager.verifyOperator(new User(1, "user1"), 1, ""));
        assertDoesNotThrow(() -> manager.verifyOperator(new User(1, "user1"), 1, null));
        assertThrows(FlowStateException.class,
                () -> manager.verifyOperator(User.withIdentity(1, "user1", "department1"), 1, null));
    }

    /**
     * 审批：以另一个身份办理同一人的待办必须被拒绝，以记录中的身份办理则放行。
     */
    @Test
    void should_reject_action_with_other_identity() {
        long recordId = createFlow("department1");
        FlowRecord record = factory.flowRecordRepository.get(recordId);
        assertEquals("department1", record.getCurrentOperatorIdentity());

        assertThrows(FlowStateException.class, () -> action(recordId, "department2"));
        // 被拒绝后记录仍为待办，用正确身份可以正常继续
        assertTrue(factory.flowRecordRepository.get(recordId).isTodo());
        assertDoesNotThrow(() -> action(recordId, "department1"));

        List<FlowRecord> bossTodos = factory.flowRecordRepository.findTodoByOperator(2);
        assertEquals(1, bossTodos.size());
    }

    /**
     * 删除：以另一个身份删除同一人的待办必须被拒绝。
     */
    @Test
    void should_reject_delete_with_other_identity() {
        long recordId = createFlow("department1");

        assertThrows(FlowStateException.class, () -> factory.flowService.delete(
                new FlowDeleteRequest(recordId, 1, "department2")));
        assertTrue(factory.flowRecordRepository.get(recordId).isTodo());

        assertDoesNotThrow(() -> factory.flowService.delete(
                new FlowDeleteRequest(recordId, 1, "department1")));
    }
}

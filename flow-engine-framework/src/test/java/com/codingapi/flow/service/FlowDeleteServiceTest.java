package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.builder.ActionBuilder;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.exception.FlowException;
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
import com.codingapi.flow.pojo.request.FlowDeleteRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 删除流程服务测试
 * <p>
 * 验证删除能力仅对「开始节点 + 待办 + 未流转 + 操作人匹配」的流程实例放行,其余场景均拒绝
 */
class FlowDeleteServiceTest {

    private MyFlowServiceFactory newFactory(User... users) {
        MyFlowServiceFactory factory = new MyFlowServiceFactory();
        for (User user : users) {
            factory.userGateway.save(user);
        }
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);
        return factory;
    }

    private Workflow buildLeaveWorkflow(MyFlowServiceFactory factory, User creator) {
        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();

        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(creator)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);
        return workflow;
    }

    private Map<String, Object> formData() {
        return Map.of("name", "lorne", "days", 1, "reason", "leave");
    }

    private long createAtStartNode(MyFlowServiceFactory factory, Workflow workflow, User user) {
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(formData());
        List<IFlowAction> startActions = workflow.getStartNode().actionManager().getActions();
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(user.getUserId());
        return factory.flowService.create(createRequest);
    }

    private List<IFlowAction> startActions(Workflow workflow) {
        return workflow.getStartNode().actionManager().getActions();
    }

    /**
     * 开始节点待办、无后续流转 → 删除成功,流程记录与待办均被清理
     */
    @Test
    void delete_start_node_todo_success() {
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        MyFlowServiceFactory factory = newFactory(user, boss);

        Workflow workflow = buildLeaveWorkflow(factory, user);
        long recordId = createAtStartNode(factory, workflow, user);

        // 删除前:存在开始节点待办
        FlowRecord before = factory.flowRecordRepository.get(recordId);
        assertNotNull(before);
        assertTrue(before.isTodo());
        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(user.getUserId()).size());
        assertEquals(1, factory.flowTodoRecordRepository.findByOperatorId(user.getUserId()).size());

        // 执行删除
        factory.flowService.delete(new FlowDeleteRequest(recordId, user.getUserId()));

        // 删除后:流程记录与待办均被清理
        assertNull(factory.flowRecordRepository.get(recordId));
        assertEquals(0, factory.flowRecordRepository.findTodoByOperator(user.getUserId()).size());
        assertEquals(0, factory.flowTodoRecordRepository.findByOperatorId(user.getUserId()).size());
    }

    /**
     * 已流转(开始节点记录变 done) → 不允许删除
     */
    @Test
    void delete_failed_when_record_already_done() {
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        MyFlowServiceFactory factory = newFactory(user, boss);

        Workflow workflow = buildLeaveWorkflow(factory, user);
        long recordId = createAtStartNode(factory, workflow, user);

        // 办理开始节点,流转到 boss
        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(formData());
        actionRequest.setRecordId(recordId);
        actionRequest.setAdvice(new FlowAdviceBody(startActions(workflow).get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(actionRequest);

        // 开始节点记录已变 done,删除应被拒绝
        assertThrows(FlowStateException.class,
                () -> factory.flowService.delete(new FlowDeleteRequest(recordId, user.getUserId())));

        // 原始数据仍在
        assertNotNull(factory.flowRecordRepository.get(recordId));
    }

    /**
     * 待办位于非开始节点(经理审批) → 不允许删除
     */
    @Test
    void delete_failed_when_not_start_node() {
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        MyFlowServiceFactory factory = newFactory(user, boss);

        Workflow workflow = buildLeaveWorkflow(factory, user);
        long recordId = createAtStartNode(factory, workflow, user);

        // 流转到 boss 节点
        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(formData());
        actionRequest.setRecordId(recordId);
        actionRequest.setAdvice(new FlowAdviceBody(startActions(workflow).get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(actionRequest);

        // boss 的待办位于审批节点,删除应被拒绝
        List<FlowRecord> bossRecords = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecords.size());
        long bossRecordId = bossRecords.get(0).getId();

        assertThrows(FlowStateException.class,
                () -> factory.flowService.delete(new FlowDeleteRequest(bossRecordId, boss.getUserId())));
    }

    /**
     * 操作人不匹配 → 不允许删除,且原始数据不变
     */
    @Test
    void delete_failed_when_operator_not_match() {
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        MyFlowServiceFactory factory = newFactory(user, boss);

        Workflow workflow = buildLeaveWorkflow(factory, user);
        long recordId = createAtStartNode(factory, workflow, user);

        // boss 不是该待办的处理人,删除应被拒绝
        assertThrows(FlowStateException.class,
                () -> factory.flowService.delete(new FlowDeleteRequest(recordId, boss.getUserId())));

        // 原始数据未被删除
        assertNotNull(factory.flowRecordRepository.get(recordId));
    }

    /**
     * 记录不存在 → 抛 FlowException
     */
    @Test
    void delete_failed_when_record_not_found() {
        User user = new User(1, "user");
        MyFlowServiceFactory factory = newFactory(user);

        assertThrows(FlowException.class,
                () -> factory.flowService.delete(new FlowDeleteRequest(99999L, user.getUserId())));
    }
}

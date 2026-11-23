package com.codingapi.flow.service;

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
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.record.FlowTodoRecord;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 流程记录身份标识落库测试（issue #288 P1）。
 *
 * <p>P1 只做身份透传与存储，不改变任何判定逻辑：身份为空时记录行为与历史完全一致。
 */
class FlowOperatorIdentityRecordTest {

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
                .build();

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
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
                                .build()))
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
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
        return Map.of("name", "lorne", "days", 1);
    }

    private List<IFlowAction> startActions(Workflow workflow) {
        return workflow.getStartNode().actionManager().getActions();
    }

    private long createAtStartNode(MyFlowServiceFactory factory, Workflow workflow, User user) {
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(formData());
        createRequest.setActionId(startActions(workflow).get(0).id());
        createRequest.setOperatorId(user.getUserId());
        return factory.flowService.create(createRequest);
    }

    /**
     * 场景：发起人带身份发起流程 → 开始节点记录与待办记录都落发起人身份。
     */
    @Test
    void should_store_creator_identity_on_start_record_and_todo() {
        User creator = User.withIdentity(1, "user1", "department1");
        User boss = User.withIdentity(2, "boss", "department2");
        MyFlowServiceFactory factory = newFactory(creator, boss);

        Workflow workflow = buildLeaveWorkflow(factory, creator);
        long recordId = createAtStartNode(factory, workflow, creator);

        FlowRecord record = factory.flowRecordRepository.get(recordId);
        assertNotNull(record);
        assertEquals("department1", record.getCreateOperatorIdentity());
        assertEquals("department1", record.getSubmitOperatorIdentity());
        assertEquals("department1", record.getCurrentOperatorIdentity());
        assertEquals("", record.getForwardOperatorIdentity());
        assertEquals("", record.getInterferedOperatorIdentity());

        List<FlowTodoRecord> todos = factory.flowTodoRecordRepository.findByOperatorId(creator.getUserId());
        assertEquals(1, todos.size());
        FlowTodoRecord todo = todos.get(0);
        assertEquals("department1", todo.getCreateOperatorIdentity());
        assertEquals("department1", todo.getSubmitOperatorIdentity());
        assertEquals("department1", todo.getCurrentOperatorIdentity());
    }

    /**
     * 场景：存量用户未实现身份 → 全部落默认身份（空串），与历史行为一致。
     */
    @Test
    void should_store_default_identity_when_operator_has_no_identity() {
        User creator = new User(1, "user1");
        User boss = new User(2, "boss");
        MyFlowServiceFactory factory = newFactory(creator, boss);

        Workflow workflow = buildLeaveWorkflow(factory, creator);
        long recordId = createAtStartNode(factory, workflow, creator);

        FlowRecord record = factory.flowRecordRepository.get(recordId);
        assertNotNull(record);
        assertTrue(record.getCreateOperatorIdentity().isEmpty());
        assertTrue(record.getSubmitOperatorIdentity().isEmpty());
        assertTrue(record.getCurrentOperatorIdentity().isEmpty());
    }

    /**
     * 场景：发起人办理后流转到审批人 → 新记录的当前审批人身份来自审批人自身，发起/提交身份保持不变。
     */
    @Test
    void should_store_approver_identity_after_pass() {
        User creator = User.withIdentity(1, "user1", "department1");
        User boss = User.withIdentity(2, "boss", "department2");
        MyFlowServiceFactory factory = newFactory(creator, boss);

        Workflow workflow = buildLeaveWorkflow(factory, creator);
        long recordId = createAtStartNode(factory, workflow, creator);

        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(formData());
        actionRequest.setRecordId(recordId);
        actionRequest.setAdvice(new FlowAdviceBody(startActions(workflow).get(0).id(), "同意", creator.getUserId()));
        factory.flowService.action(actionRequest);

        List<FlowRecord> bossTodos = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossTodos.size());
        FlowRecord bossRecord = bossTodos.get(0);
        assertEquals("department2", bossRecord.getCurrentOperatorIdentity());
        assertEquals("department1", bossRecord.getCreateOperatorIdentity());
        assertEquals("department1", bossRecord.getSubmitOperatorIdentity());

        List<FlowTodoRecord> bossTodoRecords = factory.flowTodoRecordRepository.findByOperatorId(boss.getUserId());
        assertEquals(1, bossTodoRecords.size());
        assertEquals("department2", bossTodoRecords.get(0).getCurrentOperatorIdentity());
    }
}

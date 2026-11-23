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
import com.codingapi.flow.strategy.node.RecordMergeStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 待办合并键的身份维度测试（issue #288 P2）。
 *
 * <p>核心约定（兼容追加策略）：默认身份保持历史键格式，只有非空身份才追加身份后缀，
 * 这样升级瞬间仍在运行的流程，其存量待办行仍是旧格式键，合并关系不会断裂。
 */
class FlowTodoKeyIdentityTest {

    private static final long CREATOR_ID = 9;
    private static final long APPROVER_ID = 1;

    private MyFlowServiceFactory factory;
    private Workflow workflow;

    @BeforeEach
    void setUp() {
        User creator = new User(CREATOR_ID, "creator");
        factory = new MyFlowServiceFactory();
        factory.userGateway.save(creator);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave-todo-key")
                .addField("请假人", "name", DataType.STRING)
                .build();

        FormFieldPermissionsBuilder permissions = FormFieldPermissionsBuilder.builder()
                .addPermission("leave-todo-key", "name", PermissionType.WRITE);

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(permissions.build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();

        // 开启审批人合并：同一节点、同一审批人、同一运行时快照下的记录合并成一条待办
        ApprovalNode approvalNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(permissions.build()))
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [1]}").getKey()))
                        .addStrategy(new RecordMergeStrategy(true, RecordMergeStrategy.MergeType.APPROVER))
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();

        workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave-todo-key")
                .createdOperator(creator)
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

    /**
     * 发起流程并办理，让流程流转到审批节点；返回审批节点上的待办记录。
     *
     * @param approverIdentity 审批人以哪个身份参与（通过网关切换默认用户身份生效）
     */
    private FlowRecord createAndPassToApprover(String approverIdentity) {
        // 切换审批人的默认身份：审批人由节点脚本按 id 加载（默认身份），因此这里替换网关中的用户实例
        factory.userGateway.save(User.withIdentity(APPROVER_ID, "user1", approverIdentity));

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(formData());
        createRequest.setActionId(startActions().get(0).id());
        createRequest.setOperatorId(CREATOR_ID);
        long recordId = factory.flowService.create(createRequest);

        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(formData());
        actionRequest.setRecordId(recordId);
        actionRequest.setAdvice(new FlowAdviceBody(startActions().get(0).id(), "提交", CREATOR_ID));
        factory.flowService.action(actionRequest);

        List<FlowRecord> todos = factory.flowRecordRepository.findTodoByOperator(APPROVER_ID);
        assertFalse(todos.isEmpty(), "审批节点应生成一条待办");
        // 多次调用时待办可能累积，返回本次最新生成的那条
        return todos.stream().max(Comparator.comparingLong(FlowRecord::getId)).orElseThrow();
    }

    /**
     * 默认身份：合并键必须是历史格式 {@code operatorId-workRuntimeId-nodeId}，不带任何后缀。
     */
    @Test
    void should_keep_legacy_todo_key_format_for_default_identity() {
        FlowRecord record = createAndPassToApprover("");

        assertTrue(record.isMergeable(), "审批节点应开启合并");
        String expected = String.format("%s-%s-%s",
                record.getCurrentOperatorId(), record.getWorkRuntimeId(), record.getNodeId());
        assertEquals(expected, record.getTodoKey());
        assertTrue(record.getTodoKey().indexOf('#') < 0, "默认身份不应携带身份后缀");
    }

    /**
     * 非空身份：合并键在历史格式后追加 {@code #identity}，与默认身份、其他身份天然隔离。
     */
    @Test
    void should_append_identity_suffix_for_non_default_identity() {
        FlowRecord record = createAndPassToApprover("department1");

        assertTrue(record.isMergeable(), "审批节点应开启合并");
        String expected = String.format("%s-%s-%s#department1",
                record.getCurrentOperatorId(), record.getWorkRuntimeId(), record.getNodeId());
        assertEquals(expected, record.getTodoKey());
    }

    /**
     * 同一人员的两个身份不能合并成一条待办：否则 department1 的待办会被 department2 覆盖。
     */
    @Test
    void should_not_merge_two_identities_of_same_user() {
        createAndPassToApprover("department1");
        createAndPassToApprover("department2");

        List<FlowTodoRecord> todos = factory.flowTodoRecordRepository.findByOperatorId(APPROVER_ID);
        assertEquals(2, todos.size(), "两个身份应各自保留一条待办");
        assertTrue(todos.stream().anyMatch(todo -> todo.getTodoKey().endsWith("#department1")));
        assertTrue(todos.stream().anyMatch(todo -> todo.getTodoKey().endsWith("#department2")));
    }

    /**
     * 同一身份的重复流转仍然合并：身份维度不破坏原有的合并语义。
     */
    @Test
    void should_still_merge_records_with_same_identity() {
        createAndPassToApprover("department1");
        createAndPassToApprover("department1");

        List<FlowTodoRecord> todos = factory.flowTodoRecordRepository.findByOperatorId(APPROVER_ID);
        assertEquals(1, todos.size(), "同一身份应合并为一条待办");
        assertEquals(2, todos.get(0).getMargeCount());
        assertNotNull(todos.get(0).getTodoKey());
        assertTrue(todos.get(0).getTodoKey().endsWith("#department1"));
    }
}

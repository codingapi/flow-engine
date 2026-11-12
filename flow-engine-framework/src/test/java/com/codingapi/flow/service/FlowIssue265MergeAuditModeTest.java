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
import com.codingapi.flow.strategy.node.MultiOperatorAuditStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.RecordMergeStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * issue #265：多人审批四种模式（并签 / 或签 / 顺序 / 随机一人）在开启记录合并时的验证。
 *
 * <p>流程 A-B-C，B 节点审批人为 b1、b2 且开启 {@link RecordMergeStrategy}（默认按审批人合并）。
 * 验证点：
 * <ul>
 *     <li>流程到达 B 时，b1、b2 都能在待办列表看到自己的合并待办（合并组各 1 条）；</li>
 *     <li>并签（MERGE）：未全部通过前对方的待办与合并组不应消失，全部通过后节点才完成、待办与合并关系全部清理；</li>
 *     <li>或签（ANY）：任一人通过后流程立即结束，对方待办与合并组必须被清理；</li>
 *     <li>顺序（SEQUENCE）：后序审批人的待办与合并组在激活前不产生，激活后才产生；</li>
 *     <li>随机一人（RANDOM_ONE）：仅一人收到待办，通过后流程结束且待办与合并关系清理干净。</li>
 * </ul>
 */
class FlowIssue265MergeAuditModeTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    private final User user = new User(1, "user");
    private final User b1 = new User(2, "b1");
    private final User b2 = new User(3, "b2");

    @Test
    void mergeAudit_shouldKeepBothTodosUntilAllPassed() {
        // given A(开始) - B(并签 + 合并) - C(结束)
        Workflow workflow = saveWorkflow(MultiOperatorAuditStrategy.Type.MERGE, 1.0f);
        String bNodeId = nodeIdByName(workflow, "B审批");

        // when
        long startRecordId = createAndSubmit(workflow);

        // then 流程停在 B：b1、b2 各自的合并待办组（按审批人合并，同实例内各 1 条）
        for (User approver : List.of(b1, b2)) {
            FlowTodoRecord group = singleGroupOf(approver, bNodeId);
            assertEquals(1, group.getMargeCount(), approver.getName() + " 的合并组应有 1 条记录");
            assertEquals(1, todoRecordsOf(approver, bNodeId).size(),
                    approver.getName() + " 在 B 节点应有 1 条待办记录");
            assertEquals(1, factory.flowTodoMergeRepository.findByTodoId(group.getId()).size(),
                    approver.getName() + " 应各有 1 条合并关系");
        }

        // when b1 通过（并签比例 100%，节点未完成）
        pass(todoRecordsOf(b1, bNodeId).get(0), workflow, b1);

        // then b1 待办与合并组被摘除，b2 的待办与合并组必须保留
        assertNoTodo(b1, bNodeId);
        assertEquals(0, groupsOf(b1, bNodeId).size(), "b1 的合并待办组应被删除");
        FlowTodoRecord b2Group = singleGroupOf(b2, bNodeId);
        assertEquals(1, b2Group.getMargeCount(), "b2 的合并组应保留 1 条记录");
        assertEquals(1, todoRecordsOf(b2, bNodeId).size(), "b2 的待办应保留");
        assertEquals(1, factory.flowTodoMergeRepository.findByTodoId(b2Group.getId()).size(),
                "b2 的合并关系应保留");
        assertFalse(record(startRecordId).isFinish(), "并签未全部通过时流程不应结束");

        // when b2 通过（达到 100%）
        pass(todoRecordsOf(b2, bNodeId).get(0), workflow, b2);

        // then 节点完成：待办与合并关系全部清理，流程结束
        for (User approver : List.of(b1, b2)) {
            assertNoTodo(approver, bNodeId);
            assertEquals(0, groupsOf(approver, bNodeId).size(), approver.getName() + " 的合并待办组应被删除");
        }
        assertEquals(0, factory.flowTodoMergeRepository.findAll().size(), "不应残留任何合并关系");
        assertTrue(record(startRecordId).isFinish(), "并签全部通过后流程应结束");
    }

    @Test
    void mergeAuditWithoutConfiguredPercent_shouldRequireAllOperatorsPassed() {
        // given 并签但未配置通过比例（percent=0，等价于设计器未填写比例）
        Workflow workflow = saveWorkflow(MultiOperatorAuditStrategy.Type.MERGE, 0f);
        String bNodeId = nodeIdByName(workflow, "B审批");

        // when
        long startRecordId = createAndSubmit(workflow);

        // then 两位审批人都持有待办
        assertEquals(1, todoRecordsOf(b1, bNodeId).size(), "b1 应有 1 条待办记录");
        assertEquals(1, todoRecordsOf(b2, bNodeId).size(), "b2 应有 1 条待办记录");

        // when b1 通过
        pass(todoRecordsOf(b1, bNodeId).get(0), workflow, b1);

        // then 未配置比例不应退化为或签：b2 的待办与合并组必须保留，流程不应结束
        FlowTodoRecord b2Group = singleGroupOf(b2, bNodeId);
        assertEquals(1, b2Group.getMargeCount(), "b2 的合并组应保留 1 条记录");
        assertEquals(1, todoRecordsOf(b2, bNodeId).size(), "b2 的待办应保留");
        assertFalse(record(startRecordId).isFinish(), "并签未配置比例时应按全部通过处理，流程不应提前结束");

        // when b2 通过
        pass(todoRecordsOf(b2, bNodeId).get(0), workflow, b2);

        // then 全部通过后流程结束，待办与合并关系清理干净
        for (User approver : List.of(b1, b2)) {
            assertNoTodo(approver, bNodeId);
            assertEquals(0, groupsOf(approver, bNodeId).size(),
                    approver.getName() + " 的合并待办组应被删除");
        }
        assertEquals(0, factory.flowTodoMergeRepository.findAll().size(), "不应残留任何合并关系");
        assertTrue(record(startRecordId).isFinish(), "全部通过后流程应结束");
    }

    @Test
    void anyAudit_shouldClearOtherTodoWhenOnePassed() {
        // given A(开始) - B(或签 + 合并) - C(结束)
        Workflow workflow = saveWorkflow(MultiOperatorAuditStrategy.Type.ANY, 0f);
        String bNodeId = nodeIdByName(workflow, "B审批");

        // when
        long startRecordId = createAndSubmit(workflow);

        // then b1、b2 各自都能看到自己的合并待办
        for (User approver : List.of(b1, b2)) {
            FlowTodoRecord group = singleGroupOf(approver, bNodeId);
            assertEquals(1, group.getMargeCount(), approver.getName() + " 的合并组应有 1 条记录");
        }
        assertEquals(2, factory.flowTodoMergeRepository.findAll().size(), "两个审批人各 1 条合并关系");

        FlowRecord b2Record = todoRecordsOf(b2, bNodeId).get(0);
        String b2TodoKey = b2Record.getTodoKey();

        // when b1 通过（或签：任一人通过即流转）
        pass(todoRecordsOf(b1, bNodeId).get(0), workflow, b1);

        // then 流程结束，b2 的待办、合并组与合并关系必须一起消失
        assertNoTodo(b1, bNodeId);
        assertNoTodo(b2, bNodeId);
        assertEquals(0, groupsOf(b1, bNodeId).size(), "b1 的合并待办组应被删除");
        assertEquals(0, groupsOf(b2, bNodeId).size(), "b2 的合并待办组应被删除");
        assertNull(factory.flowTodoRecordRepository.getByTodoKey(b2TodoKey),
                "b2 的合并待办行应被删除");
        assertEquals(0, factory.flowTodoMergeRepository.findAll().size(), "不应残留任何合并关系");

        FlowRecord autoDoneRecord = record(b2Record.getId());
        assertTrue(autoDoneRecord.isDone(), "b2 的记录应被自动办结");
        assertTrue(autoDoneRecord.isAutoDone(), "b2 的记录应为自动办结（无审批动作）");
        assertTrue(record(startRecordId).isFinish(), "或签任一人通过后流程应结束");
    }

    @Test
    void sequenceAudit_shouldCreateNextOperatorTodoOnlyAfterActivated() {
        // given A(开始) - B(顺序 + 合并) - C(结束)
        Workflow workflow = saveWorkflow(MultiOperatorAuditStrategy.Type.SEQUENCE, 0f);
        String bNodeId = nodeIdByName(workflow, "B审批");

        // when
        long startRecordId = createAndSubmit(workflow);

        // then 后序审批人（b2）的记录隐藏，不产生待办与合并组
        assertEquals(1, todoRecordsOf(b1, bNodeId).size(), "b1 应有 1 条待办记录");
        singleGroupOf(b1, bNodeId);
        assertEquals(0, todoRecordsOf(b2, bNodeId).size(), "b2 在未被激活前不应有 todo 记录");
        assertEquals(0, groupsOf(b2, bNodeId).size(), "b2 在未被激活前不应有合并待办组");
        assertEquals(1, factory.flowTodoMergeRepository.findAll().size(), "仅 b1 有 1 条合并关系");

        // when b1 通过
        pass(todoRecordsOf(b1, bNodeId).get(0), workflow, b1);

        // then b1 待办摘除，b2 被激活并产生自己的合并待办
        assertNoTodo(b1, bNodeId);
        assertEquals(0, groupsOf(b1, bNodeId).size(), "b1 的合并待办组应被删除");
        assertEquals(1, todoRecordsOf(b2, bNodeId).size(), "b2 通过后应被激活产生待办");
        FlowTodoRecord b2Group = singleGroupOf(b2, bNodeId);
        assertEquals(1, b2Group.getMargeCount(), "b2 的合并组应有 1 条记录");
        assertEquals(1, factory.flowTodoMergeRepository.findByTodoId(b2Group.getId()).size(),
                "b2 应有 1 条合并关系");
        assertFalse(record(startRecordId).isFinish(), "顺序审批未全部完成时流程不应结束");

        // when b2 通过
        pass(todoRecordsOf(b2, bNodeId).get(0), workflow, b2);

        // then 流程结束，待办与合并关系清理干净
        assertNoTodo(b2, bNodeId);
        assertEquals(0, groupsOf(b2, bNodeId).size(), "b2 的合并待办组应被删除");
        assertEquals(0, factory.flowTodoMergeRepository.findAll().size(), "不应残留任何合并关系");
        assertTrue(record(startRecordId).isFinish(), "顺序审批全部通过后流程应结束");
    }

    @Test
    void randomOneAudit_shouldCreateSingleOperatorTodo() {
        // given A(开始) - B(随机一人 + 合并) - C(结束)
        Workflow workflow = saveWorkflow(MultiOperatorAuditStrategy.Type.RANDOM_ONE, 0f);
        String bNodeId = nodeIdByName(workflow, "B审批");

        // when
        long startRecordId = createAndSubmit(workflow);

        // then 仅一人收到待办，且只有一条合并组
        List<User> receivers = List.of(b1, b2).stream()
                .filter(approver -> !todoRecordsOf(approver, bNodeId).isEmpty())
                .toList();
        assertEquals(1, receivers.size(), "随机一人审批时应有且仅有一名审批人收到待办");
        User receiver = receivers.get(0);
        FlowTodoRecord group = singleGroupOf(receiver, bNodeId);
        assertEquals(1, group.getMargeCount(), "接收人的合并组应有 1 条记录");
        assertEquals(1, factory.flowTodoRecordRepository.findAll().size(), "只应产生 1 条合并待办");
        assertEquals(1, factory.flowTodoMergeRepository.findAll().size(), "只应产生 1 条合并关系");

        // when 接收人通过
        pass(todoRecordsOf(receiver, bNodeId).get(0), workflow, receiver);

        // then 流程结束，待办与合并关系清理干净
        assertNoTodo(receiver, bNodeId);
        assertEquals(0, groupsOf(receiver, bNodeId).size(), "接收人的合并待办组应被删除");
        assertEquals(0, factory.flowTodoRecordRepository.findAll().size(), "不应残留任何合并待办");
        assertEquals(0, factory.flowTodoMergeRepository.findAll().size(), "不应残留任何合并关系");
        assertTrue(record(startRecordId).isFinish(), "随机一人通过后流程应结束");
    }

    // ─── 辅助方法 ───

    private Workflow saveWorkflow(MultiOperatorAuditStrategy.Type type, float percent) {
        registerUsers();
        Workflow workflow = buildWorkflow(type, percent);
        factory.workflowService.saveWorkflow(workflow);
        return workflow;
    }

    private Workflow buildWorkflow(MultiOperatorAuditStrategy.Type type, float percent) {
        FlowForm form = FlowFormBuilder.builder()
                .name("测试流程")
                .code("issue265")
                .addField("name", "name", DataType.STRING)
                .build();

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(perm())
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();

        ApprovalNode approvalNode = ApprovalNode.builder()
                .name("B审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(perm())
                        .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory
                                .createOperatorLoadScript("def run(request){return [2,3]}").getKey()))
                        .addStrategy(new MultiOperatorAuditStrategy(type, percent))
                        .addStrategy(new RecordMergeStrategy(true))
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();

        return WorkflowBuilder.builder()
                .title("测试流程")
                .code("issue265")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(approvalNode)
                .addNode(endNode)
                .build();
    }

    private FormFieldPermissionStrategy perm() {
        return new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                .addPermission("issue265", "name", PermissionType.WRITE)
                .build());
    }

    /**
     * 发起并提交到 B 节点，返回发起节点记录id
     */
    private long createAndSubmit(Workflow workflow) {
        StartNode startNode = (StartNode) workflow.getStartNode();
        IFlowAction startAction = startNode.actionManager().getActions().get(0);
        Map<String, Object> data = Map.of("name", "lorne");

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startAction.id());
        createRequest.setOperatorId(user.getUserId());
        long recordId = factory.flowService.create(createRequest);

        pass(startAction, user, data, recordId);
        return recordId;
    }

    private void registerUsers() {
        List.of(user, b1, b2).forEach(factory.userGateway::save);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);
    }

    private void pass(FlowRecord flowRecord, Workflow workflow, User operator) {
        ApprovalNode approvalNode = (ApprovalNode) workflow.getNodes().stream()
                .filter(node -> node.getName().equals("B审批"))
                .findFirst().orElseThrow();
        IFlowAction action = approvalNode.actionManager().getActions().get(0);
        pass(action, operator, Map.of("name", "lorne"), flowRecord.getId());
    }

    private void pass(IFlowAction action, User operator, Map<String, Object> data, long recordId) {
        FlowActionRequest request = new FlowActionRequest();
        request.setFormData(data);
        request.setRecordId(recordId);
        request.setAdvice(new FlowAdviceBody(action.id(), "同意", operator.getUserId()));
        factory.flowService.action(request);
    }

    private FlowRecord record(long recordId) {
        return factory.flowRecordRepository.get(recordId);
    }

    private List<FlowRecord> todoRecordsOf(User approver, String nodeId) {
        return factory.flowRecordRepository.findTodoByOperator(approver.getUserId()).stream()
                .filter(record -> nodeId.equals(record.getNodeId()))
                .toList();
    }

    private List<FlowTodoRecord> groupsOf(User approver, String nodeId) {
        return factory.flowTodoRecordRepository.findByOperatorId(approver.getUserId()).stream()
                .filter(group -> nodeId.equals(group.getNodeId()))
                .toList();
    }

    private FlowTodoRecord singleGroupOf(User approver, String nodeId) {
        List<FlowTodoRecord> groups = groupsOf(approver, nodeId);
        assertEquals(1, groups.size(), approver.getName() + " 在 B 节点应有且仅有 1 条合并待办组");
        return groups.get(0);
    }

    private void assertNoTodo(User approver, String nodeId) {
        assertEquals(0, todoRecordsOf(approver, nodeId).size(), approver.getName() + " 在 B 节点不应有 todo 记录");
    }

    private String nodeIdByName(Workflow workflow, String name) {
        return workflow.getNodes().stream()
                .filter(node -> node.getName().equals(name))
                .findFirst().orElseThrow().getId();
    }
}

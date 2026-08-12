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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 合并审批类型支持测试（issue #202）。
 *
 * <p>验证三种合并类型：审批人合并（APPROVER，默认）、发起人合并（CREATOR）、提交人合并（SUBMITTER）。
 * 场景：A-B-C 流程，a1、a2 各提交 2 条，到达 B 节点（b1 审批、开启合并）。</p>
 */
public class FlowMergeTypeSupportTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    /**
     * 发起人合并：a1/a2 各 2 条 → 按 createOperatorId 合并为 2 条待办。
     */
    @Test
    void shouldMergeByCreator() {
        // given
        Users users = prepareUsers();
        Workflow workflow = buildSimpleWorkflow(users.a1(), "mt-creator",
                "def run(request){return [2]}", true, RecordMergeStrategy.MergeType.CREATOR);
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne");

        // when 各提交 2 条
        submitAndApprove(users.a1(), workflow, data);
        submitAndApprove(users.a1(), workflow, data);
        submitAndApprove(users.a2(), workflow, data);
        submitAndApprove(users.a2(), workflow, data);

        // then B 节点 4 条记录合并为 2 条待办（a1 组、a2 组），各 margeCount=2
        assertEquals(4, factory.flowRecordRepository.findTodoByOperator(users.b1().getUserId()).size());
        List<FlowTodoRecord> todos = factory.flowTodoRecordRepository.findByOperatorId(users.b1().getUserId());
        assertEquals(2, todos.size());
        assertEquals(2, todos.get(0).getMargeCount());
        assertEquals(2, todos.get(1).getMargeCount());
        assertEquals(2, distinctMergeOperatorIds(todos));
    }

    /**
     * 提交人合并：a1/a2 各 2 条 → 按 submitOperatorId 合并为 2 条待办。
     */
    @Test
    void shouldMergeBySubmitter() {
        // given
        Users users = prepareUsers();
        Workflow workflow = buildSimpleWorkflow(users.a1(), "mt-submitter",
                "def run(request){return [2]}", true, RecordMergeStrategy.MergeType.SUBMITTER);
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne");

        // when 各提交 2 条
        submitAndApprove(users.a1(), workflow, data);
        submitAndApprove(users.a1(), workflow, data);
        submitAndApprove(users.a2(), workflow, data);
        submitAndApprove(users.a2(), workflow, data);

        // then B 节点 4 条记录合并为 2 条待办（a1 组、a2 组），各 margeCount=2
        assertEquals(4, factory.flowRecordRepository.findTodoByOperator(users.b1().getUserId()).size());
        List<FlowTodoRecord> todos = factory.flowTodoRecordRepository.findByOperatorId(users.b1().getUserId());
        assertEquals(2, todos.size());
        assertEquals(2, todos.get(0).getMargeCount());
        assertEquals(2, todos.get(1).getMargeCount());
        assertEquals(2, distinctMergeOperatorIds(todos));
    }

    /**
     * 默认（未配置 mergeType）：按审批人合并为 1 条待办，确认默认行为不回退。
     */
    @Test
    void shouldDefaultMergeByApprover() {
        // given
        Users users = prepareUsers();
        Workflow workflow = buildSimpleWorkflow(users.a1(), "mt-default",
                "def run(request){return [2]}", true, null);
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne");

        // when 各提交 2 条
        submitAndApprove(users.a1(), workflow, data);
        submitAndApprove(users.a1(), workflow, data);
        submitAndApprove(users.a2(), workflow, data);
        submitAndApprove(users.a2(), workflow, data);

        // then B 节点 4 条记录合并为 1 条待办（都按审批人 b1）
        assertEquals(4, factory.flowRecordRepository.findTodoByOperator(users.b1().getUserId()).size());
        List<FlowTodoRecord> todos = factory.flowTodoRecordRepository.findByOperatorId(users.b1().getUserId());
        assertEquals(1, todos.size());
        assertEquals(4, todos.get(0).getMargeCount());
    }

    /**
     * 多级流程 A-B-C-D：C 节点发起人合并，按 createOperatorId 分组（a1/a2）→ 2 条待办。
     */
    @Test
    void shouldMergeByCreatorOnMultiLevel() {
        // given
        Users users = prepareUsers();
        User b2 = new User(3, "b2");
        factory.userGateway.save(b2);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        Workflow workflow = buildMultiLevelWorkflow(users.a1(), "mt-creator-multi",
                "def run(request){return [2]}", "def run(request){return [3]}",
                true, RecordMergeStrategy.MergeType.CREATOR);
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne");

        // when a1/a2 各提交 2 条，B 节点 b1 全部审批通过
        for (int i = 0; i < 2; i++) {
            submitAndApprove(users.a1(), workflow, data);
            submitAndApprove(users.a2(), workflow, data);
        }
        approveAllTodo(users.b1(), workflow, data);

        // then C 节点 4 条记录，按发起人合并为 2 条待办
        assertEquals(4, factory.flowRecordRepository.findTodoByOperator(b2.getUserId()).size());
        List<FlowTodoRecord> todos = factory.flowTodoRecordRepository.findByOperatorId(b2.getUserId());
        assertEquals(2, todos.size());
        assertEquals(2, todos.get(0).getMargeCount());
        assertEquals(2, todos.get(1).getMargeCount());
        assertEquals(2, distinctMergeOperatorIds(todos));
    }

    /**
     * 多级流程 A-B-C-D：C 节点提交人合并，按 submitOperatorId 分组（都是 b1）→ 1 条待办。
     */
    @Test
    void shouldMergeBySubmitterOnMultiLevel() {
        // given
        Users users = prepareUsers();
        User b2 = new User(3, "b2");
        factory.userGateway.save(b2);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        Workflow workflow = buildMultiLevelWorkflow(users.a1(), "mt-submitter-multi",
                "def run(request){return [2]}", "def run(request){return [3]}",
                true, RecordMergeStrategy.MergeType.SUBMITTER);
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne");

        // when a1/a2 各提交 2 条，B 节点 b1 全部审批通过
        for (int i = 0; i < 2; i++) {
            submitAndApprove(users.a1(), workflow, data);
            submitAndApprove(users.a2(), workflow, data);
        }
        approveAllTodo(users.b1(), workflow, data);

        // then C 节点 4 条记录，按提交人（b1）合并为 1 条待办
        assertEquals(4, factory.flowRecordRepository.findTodoByOperator(b2.getUserId()).size());
        List<FlowTodoRecord> todos = factory.flowTodoRecordRepository.findByOperatorId(b2.getUserId());
        assertEquals(1, todos.size());
        assertEquals(4, todos.get(0).getMargeCount());
    }

    // ─── 辅助方法 ───

    private record Users(User a1, User a2, User b1) {
    }

    private Users prepareUsers() {
        User a1 = new User(1, "a1");
        User a2 = new User(4, "a2");
        User b1 = new User(2, "b1");
        factory.userGateway.save(a1);
        factory.userGateway.save(a2);
        factory.userGateway.save(b1);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);
        return new Users(a1, a2, b1);
    }

    private Workflow buildSimpleWorkflow(User createdBy, String code, String approverScript,
                                         boolean mergeable, RecordMergeStrategy.MergeType mergeType) {
        FlowForm form = FlowFormBuilder.builder()
                .name("测试流程")
                .code(code)
                .addField("name", "name", DataType.STRING)
                .build();

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission(code, "name", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();

        NodeStrategyBuilder approvalStrategies = NodeStrategyBuilder.builder()
                .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                        .addPermission(code, "name", PermissionType.WRITE)
                        .build()))
                .addStrategy(new OperatorLoadStrategy(
                        FlowGroovyScriptFactory.createOperatorLoadScript(approverScript).getKey()));
        if (mergeable) {
            approvalStrategies.addStrategy(mergeType == null
                    ? new RecordMergeStrategy(true)
                    : new RecordMergeStrategy(true, mergeType));
        }

        ApprovalNode approvalNode = ApprovalNode.builder()
                .name("审批")
                .strategies(approvalStrategies.build())
                .build();

        EndNode endNode = EndNode.builder().build();

        return WorkflowBuilder.builder()
                .title("测试流程")
                .code(code)
                .createdOperator(createdBy)
                .form(form)
                .addNode(startNode)
                .addNode(approvalNode)
                .addNode(endNode)
                .build();
    }

    private Workflow buildMultiLevelWorkflow(User createdBy, String code,
                                             String bScript, String cScript,
                                             boolean cMergeable, RecordMergeStrategy.MergeType cMergeType) {
        FlowForm form = FlowFormBuilder.builder()
                .name("测试流程")
                .code(code)
                .addField("name", "name", DataType.STRING)
                .build();

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission(code, "name", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();

        ApprovalNode bNode = ApprovalNode.builder()
                .name("B审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission(code, "name", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(bScript).getKey()))
                        .build())
                .build();

        NodeStrategyBuilder cStrategies = NodeStrategyBuilder.builder()
                .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                        .addPermission(code, "name", PermissionType.WRITE)
                        .build()))
                .addStrategy(new OperatorLoadStrategy(
                        FlowGroovyScriptFactory.createOperatorLoadScript(cScript).getKey()));
        if (cMergeable) {
            cStrategies.addStrategy(cMergeType == null
                    ? new RecordMergeStrategy(true)
                    : new RecordMergeStrategy(true, cMergeType));
        }

        ApprovalNode cNode = ApprovalNode.builder()
                .name("C审批")
                .strategies(cStrategies.build())
                .build();

        EndNode endNode = EndNode.builder().build();

        return WorkflowBuilder.builder()
                .title("测试流程")
                .code(code)
                .createdOperator(createdBy)
                .form(form)
                .addNode(startNode)
                .addNode(bNode)
                .addNode(cNode)
                .addNode(endNode)
                .build();
    }

    private void submitAndApprove(User user, Workflow workflow, Map<String, Object> data) {
        StartNode startNode = (StartNode) workflow.getStartNode();
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(user.getUserId());
        factory.flowService.create(createRequest);

        List<FlowRecord> userRecords = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        FlowRecord userRecord = userRecords.get(userRecords.size() - 1);
        approve(user, userRecord, workflow, data);
    }

    private void approveAllTodo(User approver, Workflow workflow, Map<String, Object> data) {
        List<FlowRecord> todoRecords = factory.flowRecordRepository.findTodoByOperator(approver.getUserId());
        for (FlowRecord record : todoRecords) {
            approve(approver, record, workflow, data);
        }
    }

    private void approve(User approver, FlowRecord record, Workflow workflow, Map<String, Object> data) {
        List<IFlowAction> actions = workflow.getFlowNode(record.getNodeId()).actionManager().getActions();
        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(data);
        actionRequest.setRecordId(record.getId());
        actionRequest.setAdvice(new FlowAdviceBody(actions.get(0).id(), "同意", approver.getUserId()));
        factory.flowService.action(actionRequest);
    }

    private int distinctMergeOperatorIds(List<FlowTodoRecord> todos) {
        Set<String> keys = todos.stream().map(FlowTodoRecord::getTodoKey).collect(Collectors.toSet());
        return keys.size();
    }
}
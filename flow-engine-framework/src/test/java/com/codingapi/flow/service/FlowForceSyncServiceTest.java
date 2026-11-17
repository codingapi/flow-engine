package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.builder.ActionBuilder;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.exception.FlowPermissionException;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowForceSyncRequest;
import com.codingapi.flow.pojo.response.FlowForceSyncResult;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.record.FlowTodoRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.RecordMergeStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * issue #281：运行中实例强制同步最新流程设计。
 *
 * <p>同步的语义是「换绑运行时快照」：不改写任何已有快照，只把选中实例的记录指向最新设计对应的快照，
 * 未选中的实例仍使用原快照。开启记录合并时，合并待办需要按实例拆组。</p>
 */
class FlowForceSyncServiceTest {

    private static final String WORK_CODE = "issue281";

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    /**
     * 流程管理员，强制同步仅允许流程管理员操作
     */
    private final User admin = new User(99, "admin", true);

    private final User a1 = new User(11, "a1");
    private final User a2 = new User(12, "a2");
    private final User a3 = new User(13, "a3");
    private final User b1 = new User(2, "b1");

    private final Map<String, Object> formData = Map.of("name", "lorne");

    @Test
    void should_rebind_selected_instance_and_split_merged_todo() {
        // given 三条实例停在 B 节点，b1 的待办合并为一条
        Workflow workflow = prepareWorkflow();
        String process1 = createAndSubmit(a1, workflow);
        String process2 = createAndSubmit(a2, workflow);
        String process3 = createAndSubmit(a3, workflow);

        String bNodeId = bNodeId(workflow);
        long oldRuntimeId = runtimeIdOf(process1);
        assertEquals(1, factory.flowTodoRecordRepository.findAll().size(), "应只有一条合并待办组");
        assertEquals(3, factory.flowTodoRecordRepository.findAll().get(0).getMargeCount());

        // when 只同步 process1
        renameBNode(workflow, "B审批-改名后");
        FlowForceSyncResult result = factory.flowService.forceSync(
                new FlowForceSyncRequest(WORK_CODE, List.of(process1), admin.getUserId()));

        // then 只有选中实例换绑到新快照
        long newRuntimeId = result.getWorkflowRuntimeId();
        assertNotEquals(oldRuntimeId, newRuntimeId, "应生成新的运行时快照");
        assertEquals(List.of(process1), result.getSyncedProcessIds());
        assertTrue(result.getSkippedProcessIds().isEmpty());
        assertTrue(factory.flowRecordRepository.findProcessRecords(process1).stream()
                        .allMatch(record -> record.getWorkRuntimeId() == newRuntimeId),
                "选中实例的记录应全部指向新快照");
        assertTrue(factory.flowRecordRepository.findProcessRecords(process2).stream()
                        .allMatch(record -> record.getWorkRuntimeId() == oldRuntimeId),
                "未选中实例的记录应保持原快照");

        // 旧快照内容未被修改
        WorkflowRuntime oldRuntime = factory.workflowRuntimeRepository.get(oldRuntimeId);
        assertEquals("B审批", oldRuntime.toWorkflow().getFlowNode(bNodeId).getName(), "旧快照不应被改写");
        assertEquals("B审批-改名后",
                factory.workflowRuntimeRepository.get(newRuntimeId).toWorkflow().getFlowNode(bNodeId).getName(),
                "新快照应携带最新配置");

        // 合并待办按实例拆组：旧组剩 2 条，新组 1 条
        List<FlowTodoRecord> todoGroups = factory.flowTodoRecordRepository.findAll();
        assertEquals(2, todoGroups.size(), "旧组与新组应各存在一条待办组");
        FlowTodoRecord oldGroup = findTodoGroup(todoGroups, oldRuntimeId);
        FlowTodoRecord newGroup = findTodoGroup(todoGroups, newRuntimeId);
        assertNotNull(oldGroup, "旧快照的合并待办应保留");
        assertNotNull(newGroup, "新快照应产生合并待办");
        assertEquals(2, oldGroup.getMargeCount(), "旧组应剩余 2 条实例记录");
        assertEquals(1, newGroup.getMargeCount(), "新组应包含被同步的 1 条实例记录");
        assertEquals(bNodeId, newGroup.getNodeId());
        assertEquals(process1, newGroup.getProcessId(), "新组代表记录应属于被同步实例");
    }

    @Test
    void should_rebind_all_selected_instances_and_merge_into_one_group() {
        // given
        Workflow workflow = prepareWorkflow();
        String process1 = createAndSubmit(a1, workflow);
        String process2 = createAndSubmit(a2, workflow);

        // when 同步两条实例
        renameBNode(workflow, "B审批-改名后");
        FlowForceSyncResult result = factory.flowService.forceSync(
                new FlowForceSyncRequest(WORK_CODE, List.of(process1, process2), admin.getUserId()));

        // then 两条实例共享同一份新快照，并合并为一条待办组
        long newRuntimeId = result.getWorkflowRuntimeId();
        assertEquals(2, result.getSyncedProcessIds().size());
        assertEquals(1, factory.flowTodoRecordRepository.findAll().size(), "同步后应只保留一条合并待办组");
        assertEquals(2, factory.flowTodoRecordRepository.findAll().get(0).getMargeCount());
        assertEquals(newRuntimeId, factory.flowTodoRecordRepository.findAll().get(0).getWorkflowRuntimeId());
    }

    @Test
    void should_skip_instances_already_on_latest_runtime() {
        // given 已同步过的实例
        Workflow workflow = prepareWorkflow();
        String process1 = createAndSubmit(a1, workflow);
        renameBNode(workflow, "B审批-改名后");
        FlowForceSyncResult first = factory.flowService.forceSync(
                new FlowForceSyncRequest(WORK_CODE, List.of(process1), admin.getUserId()));

        // when 再次同步同一实例
        FlowForceSyncResult second = factory.flowService.forceSync(
                new FlowForceSyncRequest(WORK_CODE, List.of(process1), admin.getUserId()));

        // then 幂等跳过
        assertEquals(first.getWorkflowRuntimeId(), second.getWorkflowRuntimeId());
        assertTrue(second.getSyncedProcessIds().isEmpty());
        assertEquals(List.of(process1), second.getSkippedProcessIds());
    }

    @Test
    void should_reject_when_node_added() {
        // given 新设计新增节点
        Workflow workflow = prepareWorkflow();
        String process1 = createAndSubmit(a1, workflow);
        long oldRuntimeId = runtimeIdOf(process1);

        updateDesign(latestDesign(), latest -> {
            List<com.codingapi.flow.node.IFlowNode> nodes = latest.getNodes();
            nodes.add(nodes.size() - 1, ApprovalNode.builder().name("新增审批").build());
        });

        // when / then 拒绝同步，且实例未被改写
        assertThrows(FlowStateException.class, () -> factory.flowService.forceSync(
                new FlowForceSyncRequest(WORK_CODE, List.of(process1), admin.getUserId())));
        assertEquals(oldRuntimeId, runtimeIdOf(process1), "校验失败时不应改写实例引用");
    }

    @Test
    void should_reject_when_node_order_changed() {
        // given 同层节点顺序调整
        Workflow workflow = prepareWorkflow();
        String process1 = createAndSubmit(a1, workflow);

        updateDesign(latestDesign(), latest -> ((BaseFlowNode) latest.getFlowNode(bNodeId(workflow))).setOrder(99));

        // when / then
        assertThrows(FlowStateException.class, () -> factory.flowService.forceSync(
                new FlowForceSyncRequest(WORK_CODE, List.of(process1), admin.getUserId())));
    }

    @Test
    void should_reject_when_action_removed() {
        // given 新设计删除了节点按钮动作
        Workflow workflow = prepareWorkflow();
        String process1 = createAndSubmit(a1, workflow);

        updateDesign(latestDesign(), latest -> ((BaseFlowNode) latest.getFlowNode(bNodeId(workflow))).setActions(List.of()));

        // when / then
        assertThrows(FlowStateException.class, () -> factory.flowService.forceSync(
                new FlowForceSyncRequest(WORK_CODE, List.of(process1), admin.getUserId())));
    }

    @Test
    void should_reject_when_instance_not_running() {
        // given 已办结的流程实例
        Workflow workflow = prepareWorkflow();
        String process1 = createAndSubmit(a1, workflow);
        long recordId = factory.flowRecordRepository.findTodoByOperator(b1.getUserId()).get(0).getId();
        approve(b1, recordId, workflow);
        assertTrue(factory.flowRecordRepository.findProcessRecords(process1).stream()
                .noneMatch(FlowRecord::isTodo), "流程应已办结");

        renameBNode(workflow, "B审批-改名后");

        // when / then
        assertThrows(FlowStateException.class, () -> factory.flowService.forceSync(
                new FlowForceSyncRequest(WORK_CODE, List.of(process1), admin.getUserId())));
    }

    @Test
    void should_reject_when_operator_not_flow_manager() {
        // given 非流程管理员发起同步
        Workflow workflow = prepareWorkflow();
        String process1 = createAndSubmit(a1, workflow);
        renameBNode(workflow, "B审批-改名后");

        // when / then
        assertThrows(FlowPermissionException.class, () -> factory.flowService.forceSync(
                new FlowForceSyncRequest(WORK_CODE, List.of(process1), a1.getUserId())));
    }

    // ─── 辅助方法 ───

    private Workflow prepareWorkflow() {
        factory.userGateway.save(admin);
        factory.userGateway.save(a1);
        factory.userGateway.save(a2);
        factory.userGateway.save(a3);
        factory.userGateway.save(b1);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        Workflow workflow = buildWorkflow(a1);
        factory.workflowService.saveWorkflow(workflow);
        return factory.workflowService.getWorkflowByCode(WORK_CODE);
    }

    private void renameBNode(Workflow workflow, String name) {
        updateDesign(latestDesign(), latest -> ((BaseFlowNode) latest.getFlowNode(bNodeId(workflow))).setName(name));
    }

    private Workflow latestDesign() {
        return factory.workflowService.getWorkflowByCode(WORK_CODE);
    }

    /**
     * 修改流程设计并保存。
     *
     * <p>运行时快照以流程设计的 {@code updatedTime} 作为版本，同一毫秒内的两次保存会复用同一份快照，
     * 因此变更前等待下一毫秒，并校验版本确实发生了变化。</p>
     */
    private Workflow updateDesign(Workflow latest, java.util.function.Consumer<Workflow> mutator) {
        long oldVersion = latest.getUpdatedTime();
        mutator.accept(latest);
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        factory.workflowService.saveWorkflow(latest);
        Workflow saved = latestDesign();
        assertNotEquals(oldVersion, saved.getUpdatedTime(), "流程设计版本时间必须变化，否则会复用旧运行时快照");
        return saved;
    }

    private long runtimeIdOf(String processId) {
        return factory.flowRecordRepository.findProcessRecords(processId).get(0).getWorkRuntimeId();
    }

    private FlowTodoRecord findTodoGroup(List<FlowTodoRecord> groups, long runtimeId) {
        return groups.stream()
                .filter(group -> group.getWorkflowRuntimeId() == runtimeId)
                .findFirst().orElse(null);
    }

    private String bNodeId(Workflow workflow) {
        return workflow.getNodes().stream()
                .filter(node -> "B审批".equals(node.getName()))
                .findFirst().orElseThrow().getId();
    }

    private Workflow buildWorkflow(User createdBy) {
        FlowForm form = FlowFormBuilder.builder()
                .name("测试流程")
                .code(WORK_CODE)
                .addField("name", "name", DataType.STRING)
                .build();

        StartNode startNode = StartNode.builder()
                .name("开始")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission(WORK_CODE, "name", PermissionType.WRITE)
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
                                .addPermission(WORK_CODE, "name", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .addStrategy(new RecordMergeStrategy(true))
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();

        return WorkflowBuilder.builder()
                .title("测试流程")
                .code(WORK_CODE)
                .createdOperator(createdBy)
                .form(form)
                .addNode(startNode)
                .addNode(bNode)
                .addNode(endNode)
                .build();
    }

    private String createAndSubmit(User user, Workflow workflow) {
        StartNode startNode = (StartNode) workflow.getStartNode();
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(WORK_CODE);
        createRequest.setFormData(formData);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(user.getUserId());
        long recordId = factory.flowService.create(createRequest);

        FlowRecord record = factory.flowRecordRepository.get(recordId);
        String processId = record.getProcessId();

        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(formData);
        actionRequest.setRecordId(recordId);
        actionRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(actionRequest);
        return processId;
    }

    private void approve(User approver, long recordId, Workflow workflow) {
        FlowRecord record = factory.flowRecordRepository.get(recordId);
        List<IFlowAction> actions = workflow.getFlowNode(record.getNodeId()).actionManager().getActions();
        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(formData);
        actionRequest.setRecordId(recordId);
        actionRequest.setAdvice(new FlowAdviceBody(actions.get(0).id(), "同意", approver.getUserId()));
        factory.flowService.action(actionRequest);
    }
}

package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.domain.FlowReissueRecord;
import com.codingapi.flow.event.FlowRecordReissueEvent;
import com.codingapi.flow.exception.FlowPermissionException;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowDetailRequest;
import com.codingapi.flow.pojo.request.FlowReissueRequest;
import com.codingapi.flow.pojo.response.FlowContent;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.service.impl.FlowReissueService;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import com.codingapi.springboot.framework.event.DomainEvent;
import com.codingapi.springboot.framework.event.IEvent;
import com.codingapi.springboot.framework.event.SpringEventInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 已完成流程二次发起场景测试（issue #272）。
 *
 * <p>流程：开始 -> 审批 -> 结束。流程正常走完后，由流程管理员对已完成的流程记录发起
 * 「二次发起」：复制源记录的表单数据创建一条全新流程，新流程停在开始节点成为发起人的待办，
 * 源流程记录保持原样不做任何改写，同时写入一条重发关系记录并推送二次发起事件。</p>
 */
class FlowReissueServiceTest {

    private static final String FORM_CODE = "reissue-form";
    private static final String WORK_CODE = "reissue-workflow";

    private MyFlowServiceFactory factory;
    private User initiator;
    private User approver;
    private User manager;
    private FlowForm form;
    private StartNode start;
    private ApprovalNode approval;

    private final List<IEvent> capturedEvents = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        factory = new MyFlowServiceFactory();
        initiator = saveUser(1, "发起人", false);
        approver = saveUser(2, "审批人", false);
        manager = saveUser(3, "流程管理员", true);
        form = FlowFormBuilder.builder()
                .name("二次发起测试表单")
                .code(FORM_CODE)
                .addField("业务内容", "content", DataType.STRING)
                .build();
        start = StartNode.builder()
                .name("开始节点")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission(FORM_CODE, "content", PermissionType.WRITE)
                                .build()))
                        .build())
                .build();
        String operatorScript = "def run(request){return [" + approver.getUserId() + "]}";
        approval = ApprovalNode.builder()
                .name("审批节点")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission(FORM_CODE, "content", PermissionType.READ)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(operatorScript).getKey()))
                        .build())
                .build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("二次发起测试流程")
                .code(WORK_CODE)
                .createdOperator(initiator)
                .form(form)
                .addNode(start)
                .addNode(approval)
                .addNode(EndNode.builder().name("结束节点").build())
                .build();
        factory.workflowService.saveWorkflow(workflow);
        initEventCapture();
    }

    @AfterEach
    void resetEventContext() throws Exception {
        Class<?> clazz = Class.forName("com.codingapi.springboot.framework.event.DomainEventContext");
        java.lang.reflect.Method getInstance = clazz.getDeclaredMethod("getInstance");
        getInstance.setAccessible(true);
        Object instance = getInstance.invoke(null);
        Field field = clazz.getDeclaredField("context");
        field.setAccessible(true);
        field.set(instance, null);
    }

    /**
     * 测试目标：流程管理员对已完成流程执行二次发起后，新流程停在开始节点成为发起人的待办。
     * 前置条件：流程已正常走完（flowState = FINISH），操作人为流程管理员。
     * 执行步骤：调用二次发起 → 查看新建记录与源记录。
     * 期望断言：新流程为独立 processId 且处于待办；待办人是原发起人；表单数据与源记录一致；
     * 源记录保持已完成、未作废、数据不变。
     */
    @Test
    void shouldCreateNewTodoFlowAtStartNodeWhenManagerReissuesFinishedFlow() {
        long sourceRecordId = finishFlow("原始数据");
        FlowRecord sourceRecord = factory.flowRecordRepository.get(sourceRecordId);
        String sourceProcessId = sourceRecord.getProcessId();

        long newRecordId = reissue(sourceRecordId, manager, null);

        FlowRecord newRecord = factory.flowRecordRepository.get(newRecordId);
        FlowRecord sourceAfter = factory.flowRecordRepository.get(sourceRecordId);
        assertAll("二次发起后的新流程与源流程",
                () -> assertNotEquals(sourceRecordId, newRecordId, "应创建一条全新的流程记录"),
                () -> assertNotEquals(sourceProcessId, newRecord.getProcessId(), "新流程应使用独立流程id"),
                () -> assertTrue(newRecord.isTodo(), "新流程应停在开始节点等待提交"),
                () -> assertEquals(start.getId(), newRecord.getNodeId(), "新流程应位于开始节点"),
                () -> assertEquals(initiator.getUserId(), newRecord.getCurrentOperatorId(),
                        "待办应回到原发起人"),
                () -> assertEquals(initiator.getUserId(), newRecord.getCreateOperatorId(),
                        "新流程的创建者应沿用原发起人"),
                () -> assertEquals("原始数据", newRecord.getFormData().get("content"),
                        "应复制源流程的表单数据"),
                () -> assertEquals(FlowRecord.SATE_FLOW_RUNNING, newRecord.getFlowState()),
                () -> assertEquals(FlowRecord.SATE_FLOW_FINISH, sourceAfter.getFlowState(),
                        "源流程状态不应被改写"),
                () -> assertFalse(sourceAfter.isRevoked(), "源流程记录不应被作废"),
                () -> assertEquals("原始数据", sourceAfter.getFormData().get("content"),
                        "源流程表单数据不应被改写"));
    }

    /**
     * 测试目标：二次发起写入重发关系记录并推送二次发起事件。
     * 前置条件：流程已正常走完，操作人为流程管理员。
     * 执行步骤：调用二次发起 → 查询关系记录与捕获的事件。
     * 期望断言：关系记录关联源记录、新记录、操作人与流程编码；事件携带源记录、新记录与操作人。
     */
    @Test
    void shouldRecordReissueRelationAndPushReissueEvent() {
        long sourceRecordId = finishFlow("原始数据");
        FlowRecord sourceRecord = factory.flowRecordRepository.get(sourceRecordId);

        long newRecordId = reissueWithAdvice(sourceRecordId, manager, "业务数据有误，重新发起");

        List<FlowReissueRecord> relations = factory.reissueRepository.findBySourceRecordId(sourceRecordId);
        assertEquals(1, relations.size(), "应写入一条重发关系记录");
        FlowReissueRecord relation = relations.get(0);
        FlowRecord newRecord = factory.flowRecordRepository.get(newRecordId);
        assertAll("重发关系记录内容",
                () -> assertEquals(sourceRecordId, relation.getSourceRecordId()),
                () -> assertEquals(sourceRecord.getProcessId(), relation.getSourceProcessId()),
                () -> assertEquals(newRecordId, relation.getNewRecordId()),
                () -> assertEquals(newRecord.getProcessId(), relation.getNewProcessId()),
                () -> assertEquals(WORK_CODE, relation.getWorkCode()),
                () -> assertEquals(manager.getUserId(), relation.getOperatorId(), "应记录执行二次发起的管理员"),
                () -> assertEquals("业务数据有误，重新发起", relation.getAdvice()));

        FlowRecordReissueEvent event = capturedEvents.stream()
                .filter(FlowRecordReissueEvent.class::isInstance)
                .map(FlowRecordReissueEvent.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("应推送二次发起事件"));
        assertAll("二次发起事件内容",
                () -> assertEquals(sourceRecordId, event.getSourceRecord().getId()),
                () -> assertEquals(newRecordId, event.getNewRecord().getId()),
                () -> assertEquals(manager.getUserId(), event.getOperator().getUserId()),
                () -> assertFalse(event.isMock()));
    }

    /**
     * 测试目标：二次发起时允许用请求中的表单数据覆盖复制的数据。
     * 前置条件：流程已正常走完。
     * 执行步骤：携带修正后的表单数据调用二次发起。
     * 期望断言：新流程使用请求中的表单数据，源流程数据保持不变。
     */
    @Test
    void shouldUseOverriddenFormDataWhenRequestProvidesIt() {
        long sourceRecordId = finishFlow("原始数据");

        long newRecordId = reissue(sourceRecordId, manager, Map.of("content", "修正后的数据"));

        assertEquals("修正后的数据",
                factory.flowRecordRepository.get(newRecordId).getFormData().get("content"));
        assertEquals("原始数据",
                factory.flowRecordRepository.get(sourceRecordId).getFormData().get("content"),
                "源流程表单数据不应被改写");
    }

    /**
     * 测试目标：二次发起后的新流程可以正常重新执行一遍。
     * 前置条件：管理员已对已完成流程执行二次发起。
     * 执行步骤：发起人提交新流程 → 审批人审批通过 → 查看新流程状态。
     * 期望断言：新流程正常结束，源流程保持已完成状态。
     */
    @Test
    void shouldRunNewFlowAgainAfterReissue() {
        long sourceRecordId = finishFlow("原始数据");
        long newRecordId = reissue(sourceRecordId, manager, null);
        FlowRecord newStartRecord = factory.flowRecordRepository.get(newRecordId);

        approve(newStartRecord, passAction(start), initiator, newStartRecord.getFormData());
        FlowRecord approvalTodo = todos(approver, approval).get(0);
        approve(approvalTodo, passAction(approval), approver, approvalTodo.getFormData());

        assertAll("新流程重新走完",
                () -> assertEquals(FlowRecord.SATE_FLOW_FINISH,
                        factory.flowRecordRepository.get(approvalTodo.getId()).getFlowState()),
                () -> assertEquals(FlowRecord.SATE_FLOW_FINISH,
                        factory.flowRecordRepository.get(sourceRecordId).getFlowState(),
                        "源流程仍保持已完成"));
    }

    /**
     * 测试目标：只有流程管理员可以执行二次发起。
     * 前置条件：流程已正常走完，操作人为普通发起人。
     * 执行步骤：普通用户调用二次发起。
     * 期望断言：抛出权限异常，且不产生新的流程记录。
     */
    @Test
    void shouldRejectReissueWhenOperatorIsNotFlowManager() {
        long sourceRecordId = finishFlow("原始数据");
        int before = factory.flowRecordRepository.findProcessRecords(
                factory.flowRecordRepository.get(sourceRecordId).getProcessId()).size();

        assertThrows(FlowPermissionException.class, () -> reissue(sourceRecordId, initiator, null));
        assertEquals(before, factory.flowRecordRepository.findProcessRecords(
                factory.flowRecordRepository.get(sourceRecordId).getProcessId()).size(),
                "被拒绝时不应产生新的流程记录");
    }

    /**
     * 测试目标：只有已正常完成的流程才允许二次发起。
     * 前置条件：流程停在开始节点待办（运行中）。
     * 执行步骤：流程管理员对运行中的记录调用二次发起。
     * 期望断言：抛出状态异常。
     */
    @Test
    void shouldRejectReissueWhenSourceRecordIsStillRunning() {
        long runningRecordId = create(Map.of("content", "运行中数据"));

        assertThrows(FlowStateException.class, () -> reissue(runningRecordId, manager, null));
    }

    /**
     * 测试目标：详情接口按「已正常完成 + 流程管理员」下发二次发起标识。
     * 前置条件：流程已正常走完。
     * 执行步骤：分别以流程管理员与普通发起人身份查看已完成流程详情。
     * 期望断言：管理员为 true，普通用户为 false。
     */
    @Test
    void shouldExposeReissueFlagOnlyForFlowManagerOnFinishedRecord() {
        long sourceRecordId = finishFlow("原始数据");

        FlowContent managerView = detail(sourceRecordId, manager);
        FlowContent initiatorView = detail(sourceRecordId, initiator);

        assertAll("详情接口的二次发起标识",
                () -> assertTrue(managerView.isReissue()),
                () -> assertFalse(initiatorView.isReissue()));
    }

    /**
     * 测试目标：终止（未通过结束）的流程不开放二次发起。
     * 前置条件：源记录流程状态为 SATE_FLOW_DONE。
     * 执行步骤：调用 canReissue 判定。
     * 期望断言：返回 false（仅 SATE_FLOW_FINISH 允许）。
     */
    @Test
    void shouldNotAllowReissueForTerminatedFlow() {
        FlowRecord terminated = Mockito.mock(FlowRecord.class);
        Mockito.when(terminated.getFlowState()).thenReturn(FlowRecord.SATE_FLOW_DONE);

        assertFalse(FlowReissueService.canReissue(terminated, manager));
    }

    private long finishFlow(String content) {
        Map<String, Object> data = Map.of("content", content);
        long recordId = create(data);
        approve(factory.flowRecordRepository.get(recordId), passAction(start), initiator, data);
        FlowRecord approvalTodo = todos(approver, approval).get(0);
        approve(approvalTodo, passAction(approval), approver, approvalTodo.getFormData());
        return recordId;
    }

    private long create(Map<String, Object> data) {
        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode(WORK_CODE);
        request.setOperatorId(initiator.getUserId());
        request.setActionId(passAction(start).id());
        request.setFormData(data);
        return factory.flowService.create(request);
    }

    private long reissue(long sourceRecordId, User operator, Map<String, Object> formData) {
        FlowReissueRequest request = new FlowReissueRequest(sourceRecordId, operator.getUserId());
        request.setFormData(formData);
        return factory.flowService.reissue(request);
    }

    private long reissueWithAdvice(long sourceRecordId, User operator, String advice) {
        FlowReissueRequest request = new FlowReissueRequest(sourceRecordId, operator.getUserId());
        request.setAdvice(advice);
        return factory.flowService.reissue(request);
    }

    private FlowContent detail(long recordId, User operator) {
        return factory.flowService.detail(
                new FlowDetailRequest(String.valueOf(recordId), operator.getUserId()));
    }

    private void approve(FlowRecord record, IFlowAction action, User operator, Map<String, Object> data) {
        FlowActionRequest request = new FlowActionRequest();
        request.setRecordId(record.getId());
        request.setFormData(data);
        request.setAdvice(new FlowAdviceBody(action.id(), "同意", operator.getUserId()));
        factory.flowService.action(request);
    }

    private List<FlowRecord> todos(User operator, IFlowNode node) {
        return factory.flowRecordRepository.findTodoByOperator(operator.getUserId()).stream()
                .filter(record -> record.getNodeId().equals(node.getId()))
                .toList();
    }

    private IFlowAction passAction(IFlowNode node) {
        return node.actionManager().getActions().stream()
                .filter(action -> "PASS".equals(action.type()))
                .findFirst()
                .orElseThrow();
    }

    private User saveUser(long id, String name, boolean manager) {
        User user = new User(id, name, manager);
        factory.userGateway.save(user);
        return user;
    }

    private void initEventCapture() throws Exception {
        ApplicationContext mockContext = Mockito.mock(ApplicationContext.class);
        Mockito.doAnswer(invocation -> {
            DomainEvent domainEvent = invocation.getArgument(0);
            capturedEvents.add(domainEvent.getEvent());
            return null;
        }).when(mockContext).publishEvent(Mockito.any(ApplicationEvent.class));
        new SpringEventInitializer(mockContext).afterPropertiesSet();
    }
}

package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.builder.ActionBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.event.FlowEventPublisher;
import com.codingapi.flow.event.FlowRecordSaveEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.event.IFlowEventPublisher;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 流程记录保存事件（issue #280）验证
 *
 * <p>引擎在流程记录保存后推送 {@link FlowRecordSaveEvent}：
 * <ul>
 *     <li>每条实际落库的记录一个事件（与 Start/Todo/Done 事件粒度一致）；</li>
 *     <li>事件的分区键为流程 {@code processId}；</li>
 *     <li>结束节点虚拟记录不落库，因此不推送保存事件；</li>
 *     <li>Mock 模式下事件 {@code isMock()} 为 {@code true}。</li>
 * </ul>
 *
 * <p>事件捕获方式：注册 {@link IFlowEventPublisher} 实现，直接收集引擎推送的 {@link IFlowEvent}。
 */
class FlowRecordSaveEventTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();
    private final List<IFlowEvent> capturedEvents = new ArrayList<>();
    private final IFlowEventPublisher publisher = capturedEvents::add;

    private User creator;
    private User approver;
    private StartNode startNode;
    private ApprovalNode approvalNode;
    private int codeSeq = 0;

    @BeforeEach
    void setUp() {
        FlowEventPublisher.register(publisher);
        creator = new User(1, "user");
        approver = new User(2, "b1");
        registerUsers(creator, approver);
    }

    @AfterEach
    void tearDown() {
        FlowEventPublisher.unregister(publisher);
    }

    @Test
    void should_push_save_event_when_save_single_record() {
        // given
        long recordId = createFlow();
        FlowRecord record = factory.flowRecordRepository.get(recordId);
        assertNotNull(record);
        capturedEvents.clear();

        // when
        factory.flowRecordService.saveFlowRecord(record);

        // then
        List<FlowRecordSaveEvent> saveEvents = saveEvents();
        assertEquals(1, saveEvents.size(), "保存单条记录应推送 1 个保存事件");
        FlowRecordSaveEvent event = saveEvents.get(0);
        assertEquals(record.getProcessId(), event.eventKey(), "保存事件的分区键应为流程 processId");
        assertEquals(recordId, event.getFlowRecord().getId(), "保存事件应携带被保存的记录");
        assertFalse(event.isMock(), "生产模式下的保存事件不应标记为 mock");
    }

    @Test
    void should_push_one_save_event_per_saved_record() {
        // given
        long firstRecordId = createFlow();
        long secondRecordId = createFlow();
        FlowRecord first = factory.flowRecordRepository.get(firstRecordId);
        FlowRecord second = factory.flowRecordRepository.get(secondRecordId);
        capturedEvents.clear();

        // when
        factory.flowRecordService.saveFlowRecords(List.of(first, second));

        // then
        List<FlowRecordSaveEvent> saveEvents = saveEvents();
        assertEquals(2, saveEvents.size(), "批量保存 2 条记录应推送 2 个保存事件");
        Set<String> processIds = saveEvents.stream().map(FlowRecordSaveEvent::eventKey).collect(Collectors.toSet());
        assertEquals(Set.of(first.getProcessId(), second.getProcessId()), processIds,
                "每个保存事件的分区键应为其对应流程的 processId");
    }

    @Test
    void should_not_push_save_event_for_end_node_virtual_record() {
        // given：走完整个流程（开始 -> B -> 结束）
        long recordId = createFlow();
        FlowRecord startTodo = factory.flowRecordRepository.get(recordId);
        pass(startTodo.getId(), startNode, creator);
        FlowRecord approvalTodo = todoOf(approver);
        pass(approvalTodo.getId(), approvalNode, approver);

        // then
        List<FlowRecordSaveEvent> saveEvents = saveEvents();
        assertFalse(saveEvents.isEmpty(), "流程流转过程中应推送过保存事件");
        assertTrue(saveEvents.stream().allMatch(event -> event.getFlowRecord().isNotEndNode()),
                "结束节点虚拟记录不落库，不应推送保存事件");
        assertTrue(saveEvents.stream().noneMatch(event -> EndNode.NODE_TYPE.equals(event.getFlowRecord().getNodeType())),
                "保存事件不应包含结束节点记录");
    }

    @Test
    void should_mark_save_event_as_mock_in_mock_mode() {
        // given
        long recordId = createFlow();
        FlowRecord record = factory.flowRecordRepository.get(recordId);
        FlowRecordService mockFlowRecordService = new FlowRecordService(
                factory.flowTodoRecordRepository,
                factory.flowTodoMergeRepository,
                factory.flowRecordRepository,
                true);
        capturedEvents.clear();

        // when
        mockFlowRecordService.saveFlowRecord(record);

        // then
        List<FlowRecordSaveEvent> saveEvents = saveEvents();
        assertEquals(1, saveEvents.size(), "Mock 模式下保存记录同样应推送 1 个保存事件");
        assertTrue(saveEvents.get(0).isMock(), "Mock 模式下的保存事件应标记为 mock");
    }

    // ==================== 工具方法 ====================

    private List<FlowRecordSaveEvent> saveEvents() {
        return capturedEvents.stream()
                .filter(FlowRecordSaveEvent.class::isInstance)
                .map(FlowRecordSaveEvent.class::cast)
                .toList();
    }

    private void registerUsers(User... users) {
        for (User user : users) {
            factory.userGateway.save(user);
        }
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);
    }

    private FlowForm form() {
        return FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .build();
    }

    private Map<String, Object> data() {
        return Map.of("name", "lorne", "days", 1);
    }

    private StartNode startNode() {
        return StartNode.builder()
                .actions(ActionBuilder.builder()
                        .addAction(com.codingapi.flow.action.actions.CustomAction.defaultAction())
                        .build())
                .build();
    }

    private ApprovalNode approvalNode() {
        return ApprovalNode.builder()
                .name("B审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .build())
                .build();
    }

    /**
     * 创建一条流程实例（开始节点待办），返回开始节点记录 id
     * <p>每次创建使用独立的工作流编码，避免同编码工作流版本覆盖影响用例
     */
    private long createFlow() {
        String code = "leave-" + (++codeSeq);
        startNode = startNode();
        approvalNode = approvalNode();
        Workflow workflow = WorkflowBuilder.builder()
                .title("审批流程")
                .code(code)
                .createdOperator(creator)
                .form(form())
                .addNode(startNode)
                .addNode(approvalNode)
                .addNode(EndNode.builder().build())
                .build();
        factory.workflowService.saveWorkflow(workflow);

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode(code);
        request.setFormData(data());
        request.setActionId(startActions.get(0).id());
        request.setOperatorId(creator.getUserId());
        return factory.flowService.create(request);
    }

    private void pass(long recordId, com.codingapi.flow.node.IFlowNode node, User operator) {
        IFlowAction action = node.actionManager().getActions().get(0);
        FlowActionRequest request = new FlowActionRequest();
        request.setFormData(data());
        request.setRecordId(recordId);
        request.setAdvice(new FlowAdviceBody(action.id(), "同意", operator.getUserId()));
        factory.flowService.action(request);
    }

    private FlowRecord todoOf(User user) {
        List<FlowRecord> list = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, list.size(), user.getName() + " 应有且仅有一条待办");
        return list.get(0);
    }
}

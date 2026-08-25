package com.codingapi.flow.service;

import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.domain.SubProcessRecord;
import com.codingapi.flow.event.FlowSubProcessResetEvent;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.ConditionBranchNode;
import com.codingapi.flow.node.nodes.ConditionElseBranchNode;
import com.codingapi.flow.node.nodes.ConditionNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.NotifyNode;
import com.codingapi.flow.node.nodes.ParallelBranchNode;
import com.codingapi.flow.node.nodes.ParallelNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.node.nodes.SubProcessNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowDetailRequest;
import com.codingapi.flow.pojo.request.FlowProcessNodeRequest;
import com.codingapi.flow.pojo.request.FlowSubProcessResetRequest;
import com.codingapi.flow.pojo.response.FlowContent;
import com.codingapi.flow.pojo.response.ProcessNode;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.script.node.SubProcessResultScript;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.SubProcessStrategy;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 子流程数据重置场景测试（issue #219）。
 *
 * <p>重置为独立接口（非审批动作）：主流程 开始 -> 子流程节点(2 实例) -> 最终审批 -> 结束，
 * 子流程节点开启 {@code resettable} 能力后，最终审批待办上调用
 * {@code FlowService.resetSubProcess} 对选中实例退回重走。</p>
 */
class FlowSubProcessResetServiceTest {

    private static final String FORM_CODE = "sub-process-reset-form";
    private static final String PARENT_CODE = "sub-process-reset-parent";
    private static final String CHILD_CODE = "sub-process-reset-child";

    private MyFlowServiceFactory factory;
    private User initiator;
    private User childOperator;
    private User finalOperator;
    private FlowForm form;
    private StartNode childStart;
    private ApprovalNode childApproval;
    private StartNode parentStart;
    private SubProcessNode subProcessNode;
    private ApprovalNode finalApproval;

    private final List<IEvent> capturedEvents = new ArrayList<>();

    @BeforeEach
    void setUp() {
        factory = new MyFlowServiceFactory();
        initiator = saveUser(1, "发起人");
        childOperator = saveUser(2, "子流程审批人");
        finalOperator = saveUser(3, "主流程最终审批人");
        form = FlowFormBuilder.builder()
                .name("子流程重置测试表单")
                .code(FORM_CODE)
                .addField("业务内容", "content", DataType.STRING)
                .build();
        saveChildWorkflow();
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
     * 测试目标：验证在最终审批节点重置选中的 1 个子流程实例后，主流程重新等待，
     * 重建实例完成后主流程恢复并可正常走完。
     * 前置条件：子流程节点开启重置能力，2 个实例全部放行，主流程停在最终审批待办。
     * 执行步骤：对最终审批待办调用重置接口（选中第 1 个实例）→ 完成重建子流程 → 审批最终节点。
     * 期望断言：旧聚合组被取代；新组含 1 个继承实例与 1 个重建实例（新 processId、
     * sourceProcessId 指向旧实例）；最终审批记录作废；重建实例完成后主流程仅恢复一次，流程正常结束。
     */
    @Test
    void shouldResetSelectedInstanceAndResumeMainFlowAfterRebuiltChildFinishes() {
        buildParentWorkflow(true);
        long parentStartRecordId = createAndSubmitParent();
        List<FlowRecord> childTodos = todos(childOperator, childApproval);
        assertEquals(2, childTodos.size());
        approve(childTodos.get(0), passAction(childApproval), childOperator, childTodos.get(0).getFormData());
        approve(childTodos.get(1), passAction(childApproval), childOperator, childTodos.get(1).getFormData());
        FlowRecord finalTodo = todos(finalOperator, finalApproval).get(0);

        SubProcessRecord passedGroup = factory.subProcessRepository
                .findByParentProcessId(parentProcessId(finalTodo)).get(0);
        String resetProcessId = passedGroup.getInstances().get(0).getProcessId();
        String keptProcessId = passedGroup.getInstances().get(1).getProcessId();

        resetSubProcess(finalTodo, List.of(resetProcessId), finalOperator);

        List<SubProcessRecord> groups = factory.subProcessRepository
                .findByParentProcessIdAndNodeId(parentProcessId(finalTodo), subProcessNode.getId());
        assertAll("重置后的聚合组状态",
                () -> assertEquals(2, groups.size(), "重置应保留旧组并新增一组"),
                () -> assertTrue(groups.get(0).isSuperseded(), "旧聚合组应标记为已取代"),
                () -> assertEquals(SubProcessRecord.State.PASSED, groups.get(0).getState(),
                        "旧聚合组状态保持放行以保留审计语义"),
                () -> assertFalse(groups.get(1).isSuperseded()),
                () -> assertEquals(SubProcessRecord.State.WAITING, groups.get(1).getState()),
                () -> assertEquals(2, groups.get(1).getTotalCount()));
        SubProcessRecord newGroup = groups.get(1);
        SubProcessRecord.Instance inherited = newGroup.getInstances().stream()
                .filter(SubProcessRecord.Instance::isInherited)
                .findFirst().orElseThrow();
        SubProcessRecord.Instance rebuilt = newGroup.getInstances().stream()
                .filter(instance -> !instance.isInherited())
                .findFirst().orElseThrow();
        assertAll("新聚合组实例构成",
                () -> assertEquals(keptProcessId, inherited.getProcessId(), "继承实例应沿用原流程id"),
                () -> assertTrue(inherited.isFinished(), "继承实例沿用原最终状态"),
                () -> assertNull(inherited.getSourceProcessId()),
                () -> assertEquals(resetProcessId, rebuilt.getSourceProcessId(),
                        "重建实例应记录其替换的旧实例流程id"),
                () -> assertNotEquals(resetProcessId, rebuilt.getProcessId(), "重建实例应为全新流程"),
                () -> assertEquals(SubProcessRecord.InstanceState.RUNNING, rebuilt.getState()));

        FlowRecord finalRecordAfterReset = factory.flowRecordRepository.get(finalTodo.getId());
        assertAll("重置后旧记录链作废",
                () -> assertTrue(finalRecordAfterReset.isRevoked(), "旧记录链应作废"),
                () -> assertFalse(finalRecordAfterReset.isTodo(), "重置后旧待办应被清理"),
                () -> assertEquals(parentStartRecordId, newGroup.getParentRecordId(),
                        "新组应沿用原触发记录作为锚点"));
        assertTrue(todos(finalOperator, finalApproval).isEmpty(), "重置后主流程应重新等待子流程");
        assertEquals(1, todos(childOperator, childApproval).size(), "重建实例应自动提交并产生子流程待办");

        FlowRecord rebuiltTodo = todos(childOperator, childApproval).get(0);
        assertEquals(rebuilt.getProcessId(), rebuiltTodo.getProcessId());
        approve(rebuiltTodo, passAction(childApproval), childOperator, rebuiltTodo.getFormData());

        List<FlowRecord> resumedTodos = todos(finalOperator, finalApproval);
        assertEquals(1, resumedTodos.size(), "重建实例完成后主流程只能恢复一次");
        FlowRecord newFinalTodo = resumedTodos.get(0);
        assertNotEquals(finalTodo.getId(), newFinalTodo.getId(), "恢复后应生成新的最终审批记录");
        assertEquals(parentStartRecordId, newFinalTodo.getFromId(), "新记录仍以触发记录为来源");

        approve(newFinalTodo, passAction(finalApproval), finalOperator, newFinalTodo.getFormData());
        assertTrue(factory.flowRecordRepository.get(newFinalTodo.getId()).isFinish(),
                "重置重走后流程应正常结束");
    }

    /**
     * 测试目标：验证选中全部实例时等价整组重建，流程仍可走完。
     * 前置条件：同主场景，主流程停在最终审批待办。
     * 执行步骤：重置全部 2 个实例，依次完成两个重建子流程，再审批最终节点。
     * 期望断言：新组无继承实例、全部为重建实例；全部完成后主流程恢复一次并正常结束。
     */
    @Test
    void shouldResetAllInstancesWhenAllSelected() {
        buildParentWorkflow(true);
        createAndSubmitParent();
        List<FlowRecord> childTodos = todos(childOperator, childApproval);
        approve(childTodos.get(0), passAction(childApproval), childOperator, childTodos.get(0).getFormData());
        approve(childTodos.get(1), passAction(childApproval), childOperator, childTodos.get(1).getFormData());
        FlowRecord finalTodo = todos(finalOperator, finalApproval).get(0);

        SubProcessRecord passedGroup = factory.subProcessRepository
                .findByParentProcessId(parentProcessId(finalTodo)).get(0);
        List<String> allProcessIds = passedGroup.getInstances().stream()
                .map(SubProcessRecord.Instance::getProcessId)
                .toList();

        resetSubProcess(finalTodo, allProcessIds, finalOperator);

        SubProcessRecord newGroup = factory.subProcessRepository
                .findByParentProcessIdAndNodeId(parentProcessId(finalTodo), subProcessNode.getId()).get(1);
        assertAll("整组重建",
                () -> assertEquals(2, newGroup.getInstances().size()),
                () -> assertTrue(newGroup.getInstances().stream().noneMatch(SubProcessRecord.Instance::isInherited)),
                () -> assertTrue(newGroup.getInstances().stream()
                        .allMatch(instance -> allProcessIds.contains(instance.getSourceProcessId()))));

        assertEquals(2, todos(childOperator, childApproval).size());
        for (FlowRecord todo : todos(childOperator, childApproval)) {
            approve(todo, passAction(childApproval), childOperator, todo.getFormData());
        }
        List<FlowRecord> resumedTodos = todos(finalOperator, finalApproval);
        assertEquals(1, resumedTodos.size());
        approve(resumedTodos.get(0), passAction(finalApproval), finalOperator, resumedTodos.get(0).getFormData());
        assertTrue(factory.flowRecordRepository.get(resumedTodos.get(0).getId()).isFinish());
    }

    /**
     * 测试目标：验证首次重置重走完成后允许再次重置（重复重置可收敛）。
     * 前置条件：主流程经一次重置重走后再次停在最终审批待办。
     * 执行步骤：第二次重置选中另一实例，完成后审批最终节点。
     * 期望断言：第二次重置基于最新聚合组，历史共产生 3 个聚合组且仅最新组有效；流程正常结束。
     */
    @Test
    void shouldSupportResetAgainAfterFirstResetCompletes() {
        buildParentWorkflow(true);
        createAndSubmitParent();
        List<FlowRecord> childTodos = todos(childOperator, childApproval);
        approve(childTodos.get(0), passAction(childApproval), childOperator, childTodos.get(0).getFormData());
        approve(childTodos.get(1), passAction(childApproval), childOperator, childTodos.get(1).getFormData());
        FlowRecord finalTodo = todos(finalOperator, finalApproval).get(0);

        SubProcessRecord firstGroup = factory.subProcessRepository
                .findByParentProcessId(parentProcessId(finalTodo)).get(0);
        String firstResetProcessId = firstGroup.getInstances().get(0).getProcessId();
        String secondResetProcessId = firstGroup.getInstances().get(1).getProcessId();
        resetSubProcess(finalTodo, List.of(firstResetProcessId), finalOperator);
        FlowRecord firstRebuiltTodo = todos(childOperator, childApproval).get(0);
        approve(firstRebuiltTodo, passAction(childApproval), childOperator, firstRebuiltTodo.getFormData());
        FlowRecord secondFinalTodo = todos(finalOperator, finalApproval).get(0);

        resetSubProcess(secondFinalTodo, List.of(secondResetProcessId), finalOperator);

        List<SubProcessRecord> groups = factory.subProcessRepository
                .findByParentProcessIdAndNodeId(parentProcessId(secondFinalTodo), subProcessNode.getId());
        assertAll("重复重置后的聚合组链",
                () -> assertEquals(3, groups.size()),
                () -> assertTrue(groups.get(0).isSuperseded()),
                () -> assertTrue(groups.get(1).isSuperseded()),
                () -> assertFalse(groups.get(2).isSuperseded()),
                () -> assertEquals(SubProcessRecord.State.WAITING, groups.get(2).getState()));
        SubProcessRecord thirdGroup = groups.get(2);
        assertEquals(secondResetProcessId,
                thirdGroup.getInstances().stream()
                        .filter(instance -> !instance.isInherited())
                        .findFirst().orElseThrow().getSourceProcessId(),
                "第二次重置应基于最新聚合组");

        FlowRecord secondRebuiltTodo = todos(childOperator, childApproval).get(0);
        approve(secondRebuiltTodo, passAction(childApproval), childOperator, secondRebuiltTodo.getFormData());
        FlowRecord thirdFinalTodo = todos(finalOperator, finalApproval).get(0);
        approve(thirdFinalTodo, passAction(finalApproval), finalOperator, thirdFinalTodo.getFormData());
        assertTrue(factory.flowRecordRepository.get(thirdFinalTodo.getId()).isFinish());
    }

    /**
     * 测试目标：验证重置推送的事件携带新旧聚合组与映射信息。
     * 前置条件：主流程停在最终审批待办，事件捕获上下文已注入。
     * 执行步骤：调用重置接口。
     * 期望断言：恰好推送 1 条重置事件；旧组已取代、新组等待；重置记录与操作人正确；
     * 新组重建实例的 sourceProcessId 与被重置旧实例对位。
     */
    @Test
    void shouldPushResetEventWithOldAndNewGroupMapping() throws Exception {
        initEventCapture();
        buildParentWorkflow(true);
        createAndSubmitParent();
        List<FlowRecord> childTodos = todos(childOperator, childApproval);
        approve(childTodos.get(0), passAction(childApproval), childOperator, childTodos.get(0).getFormData());
        approve(childTodos.get(1), passAction(childApproval), childOperator, childTodos.get(1).getFormData());
        FlowRecord finalTodo = todos(finalOperator, finalApproval).get(0);

        SubProcessRecord passedGroup = factory.subProcessRepository
                .findByParentProcessId(parentProcessId(finalTodo)).get(0);
        String resetProcessId = passedGroup.getInstances().get(0).getProcessId();
        int eventIndex = capturedEvents.size();

        resetSubProcess(finalTodo, List.of(resetProcessId), finalOperator);

        List<FlowSubProcessResetEvent> resetEvents = capturedEvents.subList(eventIndex, capturedEvents.size())
                .stream()
                .filter(FlowSubProcessResetEvent.class::isInstance)
                .map(FlowSubProcessResetEvent.class::cast)
                .toList();
        assertEquals(1, resetEvents.size(), "重置应恰好推送一条重置事件");
        FlowSubProcessResetEvent event = resetEvents.get(0);
        assertAll("重置事件载荷",
                () -> assertTrue(event.getOldRecord().isSuperseded()),
                () -> assertEquals(passedGroup.getId(), event.getOldRecord().getId()),
                () -> assertEquals(SubProcessRecord.State.WAITING, event.getNewRecord().getState()),
                () -> assertEquals(finalTodo.getId(), event.getResetRecordId()),
                () -> assertEquals(finalOperator.getUserId(), event.getResetOperator().getUserId()),
                () -> assertFalse(event.isMock()),
                () -> assertEquals(resetProcessId, event.getNewRecord().getInstances().stream()
                        .filter(instance -> !instance.isInherited())
                        .findFirst().orElseThrow().getSourceProcessId(),
                        "事件应可完成旧 -> 新实例映射"));
    }

    /**
     * 测试目标：验证重置后脚本查询只见当前有效组，旧组数据不重复计入。
     * 前置条件：主场景重置完成，重建实例尚未结束。
     * 执行步骤：完成重建实例后观察结果判定。
     * 期望断言：结果脚本按新组（继承 + 重建共 2 条最终记录）判定通过，主流程恢复，
     * 证明 superseded 组未进入脚本视野。
     */
    @Test
    void shouldOnlyExposeCurrentGroupToScriptsAfterReset() {
        buildParentWorkflow(true);
        createAndSubmitParent();
        List<FlowRecord> childTodos = todos(childOperator, childApproval);
        approve(childTodos.get(0), passAction(childApproval), childOperator, childTodos.get(0).getFormData());
        approve(childTodos.get(1), passAction(childApproval), childOperator, childTodos.get(1).getFormData());
        FlowRecord finalTodo = todos(finalOperator, finalApproval).get(0);

        SubProcessRecord passedGroup = factory.subProcessRepository
                .findByParentProcessId(parentProcessId(finalTodo)).get(0);
        resetSubProcess(finalTodo,
                List.of(passedGroup.getInstances().get(0).getProcessId()), finalOperator);

        FlowRecord rebuiltTodo = todos(childOperator, childApproval).get(0);
        approve(rebuiltTodo, passAction(childApproval), childOperator, rebuiltTodo.getFormData());

        assertEquals(1, todos(finalOperator, finalApproval).size(),
                "默认结果脚本按当前组（继承 + 重建）判定通过，旧组不应重复计入");
    }

    /**
     * 测试目标：验证详情数据在可重置时携带标识字段，不可重置时不携带。
     * 前置条件：子流程节点开启重置能力，主流程停在最终审批待办；另建未开启能力的流程。
     * 执行步骤：分别查询两个待办的流程详情。
     * 期望断言：开启能力且子流程已汇聚完成时详情携带 resetSubProcess=true；
     * 未开启能力时为 false。
     */
    @Test
    void shouldExposeResetFlagInDetailOnlyWhenResettable() {
        buildParentWorkflow(true);
        createAndSubmitParent();
        List<FlowRecord> childTodos = todos(childOperator, childApproval);
        approve(childTodos.get(0), passAction(childApproval), childOperator, childTodos.get(0).getFormData());
        approve(childTodos.get(1), passAction(childApproval), childOperator, childTodos.get(1).getFormData());
        FlowRecord finalTodo = todos(finalOperator, finalApproval).get(0);

        FlowContent content = factory.flowService.detail(
                new FlowDetailRequest(finalTodo.getId(), finalOperator.getUserId()));
        assertTrue(content.isResetSubProcess(),
                "开启重置能力且子流程已汇聚完成时，详情应携带重置标识");
    }

    /**
     * 测试目标：验证子流程未汇聚完成（主流程等待中）时详情不携带重置标识。
     * 前置条件：子流程节点开启重置能力，子流程实例尚未全部完成。
     * 执行步骤：子流程进行中时查询主流程触发记录详情。
     * 期望断言：等待中不存在可重置聚合组，标识为 false。
     */
    @Test
    void shouldNotExposeResetFlagWhileSubProcessWaiting() {
        buildParentWorkflow(true);
        long parentStartRecordId = createAndSubmitParent();
        assertEquals(2, todos(childOperator, childApproval).size());

        FlowContent content = factory.flowService.detail(
                new FlowDetailRequest(parentStartRecordId, initiator.getUserId()));
        assertFalse(content.isResetSubProcess(), "子流程等待中不应携带重置标识");
    }

    /**
     * 测试目标：验证重置后节点视图保留旧组历史并展示新组等待状态。
     * 前置条件：主场景重置完成。
     * 执行步骤：以旧最终审批记录查询节点视图。
     * 期望断言：子流程节点出现两条聚合记录——旧组已取代、新组等待中；
     * 新组含继承实例标记与重建实例映射。
     */
    @Test
    void shouldShowSupersededGroupAndWaitingGroupInProcessNodeView() {
        buildParentWorkflow(true);
        createAndSubmitParent();
        List<FlowRecord> childTodos = todos(childOperator, childApproval);
        approve(childTodos.get(0), passAction(childApproval), childOperator, childTodos.get(0).getFormData());
        approve(childTodos.get(1), passAction(childApproval), childOperator, childTodos.get(1).getFormData());
        FlowRecord finalTodo = todos(finalOperator, finalApproval).get(0);

        SubProcessRecord passedGroup = factory.subProcessRepository
                .findByParentProcessId(parentProcessId(finalTodo)).get(0);
        String resetProcessId = passedGroup.getInstances().get(0).getProcessId();
        resetSubProcess(finalTodo, List.of(resetProcessId), finalOperator);

        List<ProcessNode> nodes = factory.flowService.processNodes(new FlowProcessNodeRequest(
                finalTodo.getId(), finalOperator.getUserId(), Map.of("content", "parent")));
        List<ProcessNode> subProcessViews = nodes.stream()
                .filter(node -> subProcessNode.getId().equals(node.getNodeId()))
                .toList();
        assertEquals(2, subProcessViews.size(), "新旧两个聚合组都应在节点视图中呈现");
        ProcessNode supersededView = subProcessViews.stream()
                .filter(node -> node.getSubProcess() != null && node.getSubProcess().isSuperseded())
                .findFirst().orElseThrow();
        ProcessNode waitingView = subProcessViews.stream()
                .filter(node -> node.getSubProcess() != null && !node.getSubProcess().isSuperseded())
                .findFirst().orElseThrow();
        assertAll("节点视图聚合组展示",
                () -> assertEquals("PASSED", supersededView.getSubProcess().getState().name()),
                () -> assertEquals("WAITING", waitingView.getSubProcess().getState().name()),
                () -> assertEquals(ProcessNode.ApproveState.PROCESSING, waitingView.getApproveState()),
                () -> assertEquals(2, waitingView.getSubProcess().getTotalCount()),
                () -> assertEquals(1, waitingView.getSubProcess().getFinishedCount(),
                        "继承实例已结束应计入完成数"),
                () -> assertTrue(waitingView.getSubProcess().getInstances().stream()
                        .anyMatch(ProcessNode.SubProcessInstanceBody::isInherited)),
                () -> assertEquals(resetProcessId, waitingView.getSubProcess().getInstances().stream()
                        .filter(instance -> !instance.isInherited())
                        .findFirst().orElseThrow().getSourceProcessId()));
    }

    /**
     * 测试目标：验证子流程节点未开启重置能力时拒绝重置（默认关闭，业务可控）。
     * 前置条件：子流程节点未开启重置能力，主流程停在最终审批待办。
     * 执行步骤：调用重置接口。
     * 期望断言：抛出状态异常，不产生新聚合组。
     */
    @Test
    void shouldRejectResetWhenResettableDisabled() {
        buildParentWorkflow(false);
        createAndSubmitParent();
        List<FlowRecord> childTodos = todos(childOperator, childApproval);
        approve(childTodos.get(0), passAction(childApproval), childOperator, childTodos.get(0).getFormData());
        approve(childTodos.get(1), passAction(childApproval), childOperator, childTodos.get(1).getFormData());
        FlowRecord finalTodo = todos(finalOperator, finalApproval).get(0);

        SubProcessRecord passedGroup = factory.subProcessRepository
                .findByParentProcessId(parentProcessId(finalTodo)).get(0);
        int groupCount = factory.subProcessRepository.findByParentProcessId(parentProcessId(finalTodo)).size();

        List<String> processIds = List.of(passedGroup.getInstances().get(0).getProcessId());
        assertThrows(FlowStateException.class,
                () -> resetSubProcess(finalTodo, processIds, finalOperator));
        assertEquals(groupCount, factory.subProcessRepository.findByParentProcessId(parentProcessId(finalTodo)).size());

        FlowContent content = factory.flowService.detail(
                new FlowDetailRequest(finalTodo.getId(), finalOperator.getUserId()));
        assertFalse(content.isResetSubProcess(), "未开启重置能力时详情不应携带标识");
    }

    /**
     * 测试目标：验证选中实例不属于任何有效聚合组时拒绝重置。
     * 前置条件：主流程停在最终审批待办。
     * 执行步骤：以不存在的实例流程id调用重置。
     * 期望断言：抛出参数校验异常。
     */
    @Test
    void shouldRejectResetWhenSelectedInstanceNotInGroup() {
        buildParentWorkflow(true);
        createAndSubmitParent();
        List<FlowRecord> childTodos = todos(childOperator, childApproval);
        approve(childTodos.get(0), passAction(childApproval), childOperator, childTodos.get(0).getFormData());
        approve(childTodos.get(1), passAction(childApproval), childOperator, childTodos.get(1).getFormData());
        FlowRecord finalTodo = todos(finalOperator, finalApproval).get(0);

        assertThrows(FlowValidationException.class,
                () -> resetSubProcess(finalTodo, List.of("not-exist-process"), finalOperator));
    }

    /**
     * 测试目标：验证存在等待中的聚合组时禁止重置。
     * 前置条件：主流程停在最终审批待办；通过仓储直接补录一条等待中的聚合组模拟并行等待。
     * 执行步骤：调用重置接口。
     * 期望断言：抛出状态异常。
     */
    @Test
    void shouldRejectResetWhenAnyGroupIsWaiting() {
        buildParentWorkflow(true);
        createAndSubmitParent();
        List<FlowRecord> childTodos = todos(childOperator, childApproval);
        approve(childTodos.get(0), passAction(childApproval), childOperator, childTodos.get(0).getFormData());
        approve(childTodos.get(1), passAction(childApproval), childOperator, childTodos.get(1).getFormData());
        FlowRecord finalTodo = todos(finalOperator, finalApproval).get(0);

        SubProcessRecord passedGroup = factory.subProcessRepository
                .findByParentProcessId(parentProcessId(finalTodo)).get(0);
        factory.subProcessRepository.save(new SubProcessRecord(
                "waiting-group", finalTodo, "other-sub-node", new ArrayList<>()));

        List<String> processIds = List.of(passedGroup.getInstances().get(0).getProcessId());
        assertThrows(FlowStateException.class,
                () -> resetSubProcess(finalTodo, processIds, finalOperator));
        assertFalse(factory.subProcessRepository
                .findByParentProcessIdAndNodeId(parentProcessId(finalTodo), subProcessNode.getId())
                .get(0).isSuperseded(), "拒绝重置时旧组不能被取代");
    }

    /**
     * 测试目标：验证操作人不匹配或记录非待办时拒绝重置。
     * 前置条件：主流程停在最终审批待办。
     * 执行步骤：分别用非当前操作人、已办结记录调用重置。
     * 期望断言：均抛出状态异常。
     */
    @Test
    void shouldRejectResetWhenOperatorMismatchOrRecordNotTodo() {
        buildParentWorkflow(true);
        long parentStartRecordId = createAndSubmitParent();
        List<FlowRecord> childTodos = todos(childOperator, childApproval);
        approve(childTodos.get(0), passAction(childApproval), childOperator, childTodos.get(0).getFormData());
        approve(childTodos.get(1), passAction(childApproval), childOperator, childTodos.get(1).getFormData());
        FlowRecord finalTodo = todos(finalOperator, finalApproval).get(0);

        SubProcessRecord passedGroup = factory.subProcessRepository
                .findByParentProcessId(parentProcessId(finalTodo)).get(0);
        List<String> processIds = List.of(passedGroup.getInstances().get(0).getProcessId());

        assertThrows(FlowStateException.class,
                () -> resetSubProcess(finalTodo, processIds, childOperator),
                "非当前操作人不能重置");
        assertThrows(FlowStateException.class,
                () -> factory.flowService.resetSubProcess(new FlowSubProcessResetRequest(
                        parentStartRecordId, initiator.getUserId(), processIds)),
                "已办结记录不能重置");
    }

    /**
     * 测试目标：验证子流程后接并行分支时，合并节点锁定为第一个分支节点，
     * 仅锁定节点的待办可重置，兄弟分支被拒绝；重置作废全部分支记录链，重走后可正常走完。
     * 前置条件：主流程 开始 -> 子流程(可重置,单实例) -> 并行[分支1:审批 / 分支2:审批] -> 结束，
     * 子流程放行后两个分支待办同时存在。
     * 执行步骤：分支2待办尝试重置（拒绝）→ 分支1待办重置 → 完成重建实例 → 两个分支重新生成并依次审批。
     * 期望断言：兄弟分支重置被拒绝且详情标识为 false；锁定节点重置成功，兄弟分支记录一并作废；
     * 重建实例完成后两分支重新生成，流程正常结束。
     */
    @Test
    void shouldLockMergeNodeOnFirstBranchWhenParallelFollowsSubProcess() {
        StartNode start = writableStart("并行开始");
        SubProcessNode subProcess = resettableSubProcess(singleChildScript());
        User x1Operator = saveUser(5, "分支1审批人");
        User x2Operator = saveUser(6, "分支2审批人");
        ApprovalNode x1 = approvalNode("分支1审批", x1Operator);
        ApprovalNode x2 = approvalNode("分支2审批", x2Operator);
        ParallelBranchNode branch1 = ParallelBranchNode.builder()
                .name("分支1").order(1).blocks(x1).build();
        ParallelBranchNode branch2 = ParallelBranchNode.builder()
                .name("分支2").order(2).blocks(x2).build();
        ParallelNode parallel = ParallelNode.builder()
                .name("并行控制").blocks(branch1, branch2).build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("子流程重置测试-并行下游")
                .code(PARENT_CODE + "-parallel")
                .createdOperator(initiator)
                .form(form)
                .addNode(start)
                .addNode(subProcess)
                .addNode(parallel)
                .addNode(EndNode.builder().name("并行结束").build())
                .build();
        factory.workflowService.saveWorkflow(workflow);

        submitWorkflow(PARENT_CODE + "-parallel", start, Map.of("content", "parallel"));
        FlowRecord childTodo = todos(childOperator, childApproval).get(0);
        approve(childTodo, passAction(childApproval), childOperator, childTodo.getFormData());

        FlowRecord x1Todo = todos(x1Operator, x1).get(0);
        FlowRecord x2Todo = todos(x2Operator, x2).get(0);
        SubProcessRecord group = factory.subProcessRepository
                .findByParentProcessId(x1Todo.getProcessId()).get(0);
        List<String> processIds = List.of(group.getInstances().get(0).getProcessId());

        assertThrows(FlowStateException.class,
                () -> factory.flowService.resetSubProcess(new FlowSubProcessResetRequest(
                        x2Todo.getId(), x2Operator.getUserId(), processIds)),
                "兄弟分支不是锁定合并节点，不能重置");
        assertFalse(factory.flowService.detail(
                new FlowDetailRequest(x2Todo.getId(), x2Operator.getUserId())).isResetSubProcess(),
                "兄弟分支待办详情不应携带重置标识");
        assertTrue(factory.flowService.detail(
                new FlowDetailRequest(x1Todo.getId(), x1Operator.getUserId())).isResetSubProcess(),
                "锁定合并节点待办详情应携带重置标识");

        factory.flowService.resetSubProcess(new FlowSubProcessResetRequest(
                x1Todo.getId(), x1Operator.getUserId(), processIds));
        assertAll("锁定节点重置后的记录链",
                () -> assertTrue(factory.flowRecordRepository.get(x1Todo.getId()).isRevoked()),
                () -> assertTrue(factory.flowRecordRepository.get(x2Todo.getId()).isRevoked(),
                        "重置应作废锚点之后全部分支记录链"),
                () -> assertTrue(todos(x1Operator, x1).isEmpty()),
                () -> assertTrue(todos(x2Operator, x2).isEmpty(), "主流程重新等待子流程"));

        FlowRecord rebuiltTodo = todos(childOperator, childApproval).get(0);
        approve(rebuiltTodo, passAction(childApproval), childOperator, rebuiltTodo.getFormData());
        FlowRecord x1Todo2 = todos(x1Operator, x1).get(0);
        FlowRecord x2Todo2 = todos(x2Operator, x2).get(0);
        approve(x1Todo2, passAction(x1), x1Operator, x1Todo2.getFormData());
        approve(x2Todo2, passAction(x2), x2Operator, x2Todo2.getFormData());
        assertTrue(factory.flowRecordRepository.get(x2Todo2.getId()).isFinish(),
                "重走后并行分支汇聚流程应正常结束");
    }

    /**
     * 测试目标：验证子流程后接条件分支时，实际命中分支的首个节点即为锁定合并节点，可重置。
     * 前置条件：主流程 开始 -> 子流程(可重置,单实例) -> 条件[主分支:审批 / ELSE:审批] -> 结束，
     * 以不满足主分支条件的数据发起，命中 ELSE 分支。
     * 执行步骤：完成子流程 → ELSE 分支待办上重置 → 完成重建实例 → 再次审批 ELSE 分支。
     * 期望断言：主分支不产生待办；ELSE 分支可重置且详情标识为 true；重走后流程正常结束。
     */
    @Test
    void shouldAllowResetFromActuallyTakenConditionBranch() {
        StartNode start = writableStart("条件开始");
        SubProcessNode subProcess = resettableSubProcess(singleChildScript());
        User primaryOperator = saveUser(7, "主分支审批人");
        User elseOperator = saveUser(8, "ELSE分支审批人");
        ApprovalNode primaryApproval = approvalNode("主分支审批", primaryOperator);
        ApprovalNode elseApproval = approvalNode("ELSE分支审批", elseOperator);
        ConditionBranchNode primaryBranch = ConditionBranchNode.builder()
                .name("条件主分支").order(1)
                .conditionScript(FlowGroovyScriptFactory.createConditionScript(
                        "def run(request){return request.getFormData('content') == 'PRIMARY'}").getKey())
                .blocks(primaryApproval).build();
        ConditionElseBranchNode elseBranch = ConditionElseBranchNode.builder()
                .name("条件ELSE分支").blocks(elseApproval).build();
        ConditionNode condition = ConditionNode.builder()
                .name("条件").blocks(primaryBranch, elseBranch).build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("子流程重置测试-条件下游")
                .code(PARENT_CODE + "-condition")
                .createdOperator(initiator)
                .form(form)
                .addNode(start)
                .addNode(subProcess)
                .addNode(condition)
                .addNode(EndNode.builder().name("条件结束").build())
                .build();
        factory.workflowService.saveWorkflow(workflow);

        submitWorkflow(PARENT_CODE + "-condition", start, Map.of("content", "OTHER"));
        FlowRecord childTodo = todos(childOperator, childApproval).get(0);
        approve(childTodo, passAction(childApproval), childOperator, childTodo.getFormData());

        assertTrue(todos(primaryOperator, primaryApproval).isEmpty(), "未命中分支不应产生待办");
        FlowRecord elseTodo = todos(elseOperator, elseApproval).get(0);
        assertTrue(factory.flowService.detail(
                new FlowDetailRequest(elseTodo.getId(), elseOperator.getUserId())).isResetSubProcess(),
                "实际命中分支的首个节点应携带重置标识");

        SubProcessRecord group = factory.subProcessRepository
                .findByParentProcessId(elseTodo.getProcessId()).get(0);
        factory.flowService.resetSubProcess(new FlowSubProcessResetRequest(
                elseTodo.getId(), elseOperator.getUserId(),
                List.of(group.getInstances().get(0).getProcessId())));
        assertTrue(factory.flowRecordRepository.get(elseTodo.getId()).isRevoked());

        FlowRecord rebuiltTodo = todos(childOperator, childApproval).get(0);
        approve(rebuiltTodo, passAction(childApproval), childOperator, rebuiltTodo.getFormData());
        FlowRecord elseTodo2 = todos(elseOperator, elseApproval).get(0);
        approve(elseTodo2, passAction(elseApproval), elseOperator, elseTodo2.getFormData());
        assertTrue(factory.flowRecordRepository.get(elseTodo2.getId()).isFinish(),
                "重走后条件分支流程应正常结束");
    }

    /**
     * 测试目标：验证重置仅允许在锁定合并节点发起，合并节点之后的更深层节点不可重置。
     * 前置条件：主流程 开始 -> 子流程(可重置,单实例) -> 第一审批 -> 第二审批 -> 结束，
     * 流程已推进到第二审批待办。
     * 执行步骤：在第二审批待办上调用重置。
     * 期望断言：抛出状态异常，详情标识为 false。
     */
    @Test
    void shouldRejectResetFromNodeDeeperThanLockedMergeNode() {
        StartNode start = writableStart("深层开始");
        SubProcessNode subProcess = resettableSubProcess(singleChildScript());
        User firstOperator = saveUser(9, "第一审批人");
        User secondOperator = saveUser(10, "第二审批人");
        ApprovalNode firstApproval = approvalNode("第一审批", firstOperator);
        ApprovalNode secondApproval = approvalNode("第二审批", secondOperator);
        Workflow workflow = WorkflowBuilder.builder()
                .title("子流程重置测试-深层节点")
                .code(PARENT_CODE + "-deep")
                .createdOperator(initiator)
                .form(form)
                .addNode(start)
                .addNode(subProcess)
                .addNode(firstApproval)
                .addNode(secondApproval)
                .addNode(EndNode.builder().name("深层结束").build())
                .build();
        factory.workflowService.saveWorkflow(workflow);

        submitWorkflow(PARENT_CODE + "-deep", start, Map.of("content", "deep"));
        FlowRecord childTodo = todos(childOperator, childApproval).get(0);
        approve(childTodo, passAction(childApproval), childOperator, childTodo.getFormData());

        FlowRecord firstTodo = todos(firstOperator, firstApproval).get(0);
        assertTrue(factory.flowService.detail(
                new FlowDetailRequest(firstTodo.getId(), firstOperator.getUserId())).isResetSubProcess(),
                "锁定合并节点（第一审批）应携带重置标识");
        approve(firstTodo, passAction(firstApproval), firstOperator, firstTodo.getFormData());

        FlowRecord secondTodo = todos(secondOperator, secondApproval).get(0);
        SubProcessRecord group = factory.subProcessRepository
                .findByParentProcessId(secondTodo.getProcessId()).stream()
                .filter(record -> !record.isSuperseded())
                .findFirst().orElseThrow();
        assertThrows(FlowStateException.class,
                () -> factory.flowService.resetSubProcess(new FlowSubProcessResetRequest(
                        secondTodo.getId(), secondOperator.getUserId(),
                        List.of(group.getInstances().get(0).getProcessId()))),
                "合并节点之后的更深层节点不能重置");
        assertFalse(factory.flowService.detail(
                new FlowDetailRequest(secondTodo.getId(), secondOperator.getUserId())).isResetSubProcess());
    }

    /**
     * 测试目标：验证子流程与合并节点之间存在抄送节点时，抄送记录不影响锁定合并节点判定。
     * 前置条件：主流程 开始 -> 子流程(可重置,单实例) -> 抄送 -> 审批 -> 结束。
     * 执行步骤：完成子流程 → 审批待办上重置 → 完成重建实例 → 审批。
     * 期望断言：抄送记录产生但不参与锁定判定，审批节点可重置；重走后流程正常结束。
     */
    @Test
    void shouldAllowResetWhenNotifyNodeBeforeMergeNode() {
        StartNode start = writableStart("抄送开始");
        SubProcessNode subProcess = resettableSubProcess(singleChildScript());
        User notifyOperator = saveUser(11, "抄送接收人");
        NotifyNode notify = NotifyNode.builder()
                .name("抄送")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(readonlyPermission())
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(
                                        "def run(request){return [" + notifyOperator.getUserId() + "]}").getKey()))
                        .build())
                .build();
        ApprovalNode approval = approvalNode("抄送后审批", finalOperator);
        Workflow workflow = WorkflowBuilder.builder()
                .title("子流程重置测试-抄送下游")
                .code(PARENT_CODE + "-notify")
                .createdOperator(initiator)
                .form(form)
                .addNode(start)
                .addNode(subProcess)
                .addNode(notify)
                .addNode(approval)
                .addNode(EndNode.builder().name("抄送结束").build())
                .build();
        factory.workflowService.saveWorkflow(workflow);

        submitWorkflow(PARENT_CODE + "-notify", start, Map.of("content", "notify"));
        FlowRecord childTodo = todos(childOperator, childApproval).get(0);
        approve(childTodo, passAction(childApproval), childOperator, childTodo.getFormData());

        FlowRecord approvalTodo = todos(finalOperator, approval).get(0);
        assertTrue(factory.flowService.detail(
                new FlowDetailRequest(approvalTodo.getId(), finalOperator.getUserId())).isResetSubProcess(),
                "抄送节点直通后，锁定合并节点为审批节点");

        SubProcessRecord group = factory.subProcessRepository
                .findByParentProcessId(approvalTodo.getProcessId()).get(0);
        factory.flowService.resetSubProcess(new FlowSubProcessResetRequest(
                approvalTodo.getId(), finalOperator.getUserId(),
                List.of(group.getInstances().get(0).getProcessId())));
        assertTrue(factory.flowRecordRepository.get(approvalTodo.getId()).isRevoked());

        FlowRecord rebuiltTodo = todos(childOperator, childApproval).get(0);
        approve(rebuiltTodo, passAction(childApproval), childOperator, rebuiltTodo.getFormData());
        FlowRecord approvalTodo2 = todos(finalOperator, approval).get(0);
        approve(approvalTodo2, passAction(approval), finalOperator, approvalTodo2.getFormData());
        assertTrue(factory.flowRecordRepository.get(approvalTodo2.getId()).isFinish(),
                "重走后流程应正常结束");
    }

    /**
     * 测试目标：验证重建实例同步跑完（纯自动子流程）时，主流程不会因作废范围
     * 误伤新恢复的记录而卡死。
     * 前置条件：主流程 开始 -> 子流程(可重置,单实例) -> 最终审批 -> 结束；
     * 子流程为纯自动流程（开始 -> 结束），放行后同步完成。
     * 执行步骤：主流程停在最终审批待办后调用重置接口。
     * 期望断言：重置后主流程恢复出有效的最终审批待办且未被作废，新聚合组放行，流程不卡死。
     */
    @Test
    void shouldNotInvalidateResumedRecordsWhenRebuiltChildCompletesSynchronously() {
        StartNode autoStart = writableStart("自动子流程开始");
        Workflow autoWorkflow = WorkflowBuilder.builder()
                .title("子流程重置测试-自动子流程")
                .code(CHILD_CODE + "-auto")
                .createdOperator(initiator)
                .form(form)
                .addNode(autoStart)
                .addNode(EndNode.builder().name("自动子流程结束").build())
                .build();
        factory.workflowService.saveWorkflow(autoWorkflow);
        String autoScript = """
                def run(request){
                    return request.toCreateRequest('%s', %d, '%s', [content:'auto-child'])
                }
                """.formatted(CHILD_CODE + "-auto", initiator.getUserId(), passAction(autoStart).id());

        StartNode start = writableStart("同步开始");
        SubProcessNode subProcess = resettableSubProcess(autoScript);
        ApprovalNode syncFinalApproval = approvalNode("同步最终审批", finalOperator);
        Workflow workflow = WorkflowBuilder.builder()
                .title("子流程重置测试-同步完成")
                .code(PARENT_CODE + "-sync")
                .createdOperator(initiator)
                .form(form)
                .addNode(start)
                .addNode(subProcess)
                .addNode(syncFinalApproval)
                .addNode(EndNode.builder().name("同步结束").build())
                .build();
        factory.workflowService.saveWorkflow(workflow);

        submitWorkflow(PARENT_CODE + "-sync", start, Map.of("content", "sync"));
        FlowRecord finalTodo = todos(finalOperator, syncFinalApproval).get(0);

        SubProcessRecord group = factory.subProcessRepository
                .findByParentProcessId(finalTodo.getProcessId()).get(0);
        factory.flowService.resetSubProcess(new FlowSubProcessResetRequest(
                finalTodo.getId(), finalOperator.getUserId(),
                List.of(group.getInstances().get(0).getProcessId())));

        List<FlowRecord> resumedTodos = todos(finalOperator, syncFinalApproval);
        assertEquals(1, resumedTodos.size(),
                "重建实例同步完成后主流程应恢复出有效待办，不能卡死");
        assertFalse(resumedTodos.get(0).isRevoked(), "新恢复的记录不能被误作废");
        SubProcessRecord latestGroup = factory.subProcessRepository
                .findByParentProcessIdAndNodeId(finalTodo.getProcessId(), subProcess.getId()).stream()
                .filter(record -> !record.isSuperseded())
                .findFirst().orElseThrow();
        assertEquals(SubProcessRecord.State.PASSED, latestGroup.getState(),
                "同步完成的新聚合组应放行");
    }

    // ==================== 流程构建与操作辅助 ====================

    private SubProcessNode resettableSubProcess(String script) {
        return SubProcessNode.builder()
                .name("可重置子流程")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy(
                                FlowGroovyScriptFactory.createSubProcessScript(script).getKey(),
                                true,
                                SubProcessResultScript.defaultScript().getScript(),
                                false,
                                true))
                        .build())
                .build();
    }

    private String singleChildScript() {
        return """
                def run(request){
                    return request.toCreateRequest('%s', %d, '%s', [content:'only-child'])
                }
                """.formatted(CHILD_CODE, initiator.getUserId(), passAction(childStart).id());
    }

    private long submitWorkflow(String code, StartNode start, Map<String, Object> data) {
        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode(code);
        request.setOperatorId(initiator.getUserId());
        request.setActionId(passAction(start).id());
        request.setFormData(data);
        long recordId = factory.flowService.create(request);
        approve(factory.flowRecordRepository.get(recordId), passAction(start), initiator, data);
        return recordId;
    }

    private void buildParentWorkflow(boolean resettable) {
        parentStart = writableStart("主流程开始");
        subProcessNode = SubProcessNode.builder()
                .name("批量子流程")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy(
                                FlowGroovyScriptFactory.createSubProcessScript(createScript()).getKey(),
                                true,
                                SubProcessResultScript.defaultScript().getScript(),
                                false,
                                resettable))
                        .build())
                .build();
        finalApproval = approvalNode("最终审批", finalOperator);
        Workflow parentWorkflow = WorkflowBuilder.builder()
                .title("子流程重置测试-主流程")
                .code(PARENT_CODE)
                .createdOperator(initiator)
                .form(form)
                .addNode(parentStart)
                .addNode(subProcessNode)
                .addNode(finalApproval)
                .addNode(EndNode.builder().name("主流程结束").build())
                .build();
        factory.workflowService.saveWorkflow(parentWorkflow);
    }

    private String createScript() {
        return """
                def run(request){
                    return [
                        request.toCreateRequest('%s', %d, '%s', [content:'child-1']),
                        request.toCreateRequest('%s', %d, '%s', [content:'child-2'])
                    ]
                }
                """.formatted(CHILD_CODE, initiator.getUserId(), passAction(childStart).id(),
                CHILD_CODE, initiator.getUserId(), passAction(childStart).id());
    }

    private void saveChildWorkflow() {
        childStart = writableStart("子流程开始");
        childApproval = approvalNode("子流程审批", childOperator);
        Workflow childWorkflow = WorkflowBuilder.builder()
                .title("子流程重置测试-子流程")
                .code(CHILD_CODE)
                .createdOperator(initiator)
                .form(form)
                .addNode(childStart)
                .addNode(childApproval)
                .addNode(EndNode.builder().name("子流程结束").build())
                .build();
        factory.workflowService.saveWorkflow(childWorkflow);
    }

    private long createAndSubmitParent() {
        Map<String, Object> data = Map.of("content", "parent");
        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode(PARENT_CODE);
        request.setOperatorId(initiator.getUserId());
        request.setActionId(passAction(parentStart).id());
        request.setFormData(data);
        long recordId = factory.flowService.create(request);
        approve(factory.flowRecordRepository.get(recordId), passAction(parentStart), initiator, data);
        return recordId;
    }

    private void resetSubProcess(FlowRecord record, List<String> processIds, User operator) {
        factory.flowService.resetSubProcess(
                new FlowSubProcessResetRequest(record.getId(), operator.getUserId(), processIds));
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

    private String parentProcessId(FlowRecord record) {
        return record.getProcessId();
    }

    private StartNode writableStart(String name) {
        return StartNode.builder()
                .name(name)
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission(FORM_CODE, "content", PermissionType.WRITE)
                                .build()))
                        .build())
                .build();
    }

    private ApprovalNode approvalNode(String name, User operator) {
        String script = "def run(request){return [" + operator.getUserId() + "]}";
        return ApprovalNode.builder()
                .name(name)
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(readonlyPermission())
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(script).getKey()))
                        .build())
                .build();
    }

    private FormFieldPermissionStrategy readonlyPermission() {
        return new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                .addPermission(FORM_CODE, "content", PermissionType.READ)
                .build());
    }

    private User saveUser(long id, String name) {
        User user = new User(id, name);
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

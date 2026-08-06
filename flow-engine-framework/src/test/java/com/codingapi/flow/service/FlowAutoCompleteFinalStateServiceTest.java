package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.LoopTriggerTraceContext;
import com.codingapi.flow.domain.DelayTask;
import com.codingapi.flow.factory.CopyOnReadFlowServiceFactory;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.DelayNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.NotifyNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.node.nodes.SubProcessNode;
import com.codingapi.flow.node.nodes.TriggerNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.SubProcessStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 流程自动完成场景下最终流程状态验证（issue #184）。
 * <p>
 * 覆盖：主流程 A(开始)->SubProcess->B(结束)、子流程 SA(开始)->SB(抄送)->SC(结束) 的全自动完成链路；
 * 以及 A-B-C（B 为触发节点）、包含延迟节点、仅有 A-B（开始->结束）等自动完成场景，
 * 验证流程自动走完后所有流程记录最终状态为"完成"而非停留在"运行中"。
 */
class FlowAutoCompleteFinalStateServiceTest {

    private static final String FORM_CODE = "auto-complete-form";
    private static final String PARENT_CODE = "auto-complete-parent";
    private static final String CHILD_CODE = "auto-complete-child";

    private MyFlowServiceFactory factory;
    private User initiator;
    private FlowForm form;

    @BeforeEach
    void setUp() {
        factory = new MyFlowServiceFactory();
        LoopTriggerTraceContext.getInstance().clear();
        initiator = saveUser(1, "发起人");
        form = FlowFormBuilder.builder()
                .name("自动完成测试表单")
                .code(FORM_CODE)
                .addField("业务内容", "content", DataType.STRING)
                .build();
    }

    /**
     * 测试目标：主流程 A(开始)->SubProcess->B(结束)，子流程 SA(开始)->SB(抄送)->SC(结束)，
     * 发起主流程后子流程自动创建并自动提交，抄送节点自动完成，随后主流程恢复并走到结束节点。
     * 前置条件：子流程节点开启创建后自动提交（submit=true），子流程抄送节点有操作人。
     * 执行步骤：提交主流程开始节点，整个链路自动流转。
     * 期望断言：主流程全部记录 isFinish()，无残留待办；子流程全部记录同样完成。
     */
    @Test
    void shouldFinishParentFlowWhenSubProcessWithNotifyAutoCompletes() {
        StartNode childStart = writableStart("子流程开始");
        NotifyNode childNotify = NotifyNode.builder()
                .name("子流程抄送")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(
                                        "def run(request){ return [" + initiator.getUserId() + "] }").getKey()))
                        .build())
                .build();
        saveChildWorkflow(childStart, childNotify);

        StartNode parentStart = writableStart("主流程开始");
        String createScript = "def run(request){ return request.toCreateRequest('%s', %d, '%s', request.getFormData()) }"
                .formatted(CHILD_CODE, initiator.getUserId(), passAction(childStart).id());
        SubProcessNode subProcess = SubProcessNode.builder()
                .name("子流程节点")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy(
                                FlowGroovyScriptFactory.createSubProcessScript(createScript).getKey(), true))
                        .build())
                .build();
        saveParentWorkflow(parentStart, subProcess);

        long parentRecordId = createAndSubmitParent(parentStart);

        FlowRecord parentStartRecord = factory.flowRecordRepository.get(parentRecordId);
        List<FlowRecord> parentRecords = records(parentStartRecord.getProcessId());
        String childProcessId = factory.subProcessRepository
                .findByParentRecordId(parentRecordId).get(0).getInstances().get(0).getProcessId();
        List<FlowRecord> childRecords = records(childProcessId);
        assertAll("主流程自动完成后的最终状态",
                () -> assertTrue(parentRecords.stream().allMatch(FlowRecord::isFinish),
                        "主流程自动完成后所有记录都应标记为完成，不能停留在运行中"),
                () -> assertEquals(0, parentRecords.stream().filter(FlowRecord::isTodo).count(),
                        "主流程完成后不应残留待办"),
                () -> assertEquals(0, factory.flowRecordRepository.findTodoByOperator(initiator.getUserId()).size(),
                        "主流程完成后发起人不应再有待办"),
                () -> assertTrue(childRecords.stream().allMatch(FlowRecord::isFinish),
                        "子流程自动完成后所有记录都应标记为完成"));
    }

    /**
     * 测试目标：子流程仅 SA(开始)->SC(结束) 时，全自动完成后主流程最终记录状态为完成。
     * 前置条件：子流程只有开始与结束节点，子流程节点开启自动提交。
     * 执行步骤：提交主流程开始节点。
     * 期望断言：主流程全部记录 isFinish()，无残留待办。
     */
    @Test
    void shouldFinishParentFlowWhenSubProcessIsOnlyStartAndEnd() {
        StartNode childStart = writableStart("简洁子流程开始");
        saveChildWorkflow(childStart, null);

        StartNode parentStart = writableStart("简洁主流程开始");
        String createScript = "def run(request){ return request.toCreateRequest('%s', %d, '%s', request.getFormData()) }"
                .formatted(CHILD_CODE, initiator.getUserId(), passAction(childStart).id());
        SubProcessNode subProcess = SubProcessNode.builder()
                .name("简洁子流程节点")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy(
                                FlowGroovyScriptFactory.createSubProcessScript(createScript).getKey(), true))
                        .build())
                .build();
        saveParentWorkflow(parentStart, subProcess);

        long parentRecordId = createAndSubmitParent(parentStart);

        FlowRecord parentStartRecord = factory.flowRecordRepository.get(parentRecordId);
        List<FlowRecord> parentRecords = records(parentStartRecord.getProcessId());
        assertAll("仅有开始结束的子流程自动完成后的主流程状态",
                () -> assertTrue(parentRecords.stream().allMatch(FlowRecord::isFinish),
                        "主流程所有记录都应标记为完成"),
                () -> assertEquals(0, parentRecords.stream().filter(FlowRecord::isTodo).count(),
                        "主流程完成后不应残留待办"));
    }

    /**
     * 测试目标：A(开始)->B(触发)->C(结束)，B 为触发节点，自动执行后最终流程状态为完成。
     * 前置条件：触发节点使用默认触发脚本。
     * 执行步骤：提交 A 开始节点，触发节点自动执行并进入 C 结束节点。
     * 期望断言：流程全部记录 isFinish()，无残留待办。
     */
    @Test
    void shouldFinishFlowWhenTriggerNodeInMiddle() {
        StartNode start = writableStart("触发链开始");
        TriggerNode trigger = TriggerNode.builder()
                .name("触发节点")
                .build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("触发节点自动完成流程")
                .code("auto-complete-trigger")
                .createdOperator(initiator)
                .form(form)
                .addNode(start)
                .addNode(trigger)
                .addNode(EndNode.builder().name("结束").build())
                .build();
        factory.workflowService.saveWorkflow(workflow);

        long startRecordId = createAndSubmit(start, "auto-complete-trigger");

        List<FlowRecord> flowRecords = records(factory.flowRecordRepository.get(startRecordId).getProcessId());
        assertAll("触发节点自动完成后的最终状态",
                () -> assertTrue(flowRecords.stream().allMatch(FlowRecord::isFinish),
                        "触发节点自动完成后所有记录都应标记为完成"),
                () -> assertEquals(0, flowRecords.stream().filter(FlowRecord::isTodo).count(),
                        "触发节点流程完成后不应残留待办"));
    }

    /**
     * 测试目标：A(开始)->B(延迟)->C(结束)，延迟任务触发后最终流程状态为完成。
     * 前置条件：延迟节点使用默认延迟策略。
     * 执行步骤：提交 A 开始节点，延迟任务注册；手动触发延迟任务。
     * 期望断言：延迟任务触发后流程全部记录 isFinish()，无残留待办。
     */
    @Test
    void shouldFinishFlowWhenDelayNodeInMiddle() {
        StartNode start = writableStart("延迟链开始");
        DelayNode delay = DelayNode.builder()
                .name("延迟节点")
                .build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("延迟节点自动完成流程")
                .code("auto-complete-delay")
                .createdOperator(initiator)
                .form(form)
                .addNode(start)
                .addNode(delay)
                .addNode(EndNode.builder().name("结束").build())
                .build();
        factory.workflowService.saveWorkflow(workflow);

        long startRecordId = createAndSubmit(start, "auto-complete-delay");

        List<DelayTask> delayTasks = factory.delayTaskRepository.findAll();
        assertEquals(1, delayTasks.size(), "提交开始节点后应注册一个延迟任务");
        factory.repositoryHolder.createDelayTriggerService(delayTasks.get(0)).trigger();

        List<FlowRecord> flowRecords = records(factory.flowRecordRepository.get(startRecordId).getProcessId());
        assertAll("延迟任务触发后的最终状态",
                () -> assertTrue(flowRecords.stream().allMatch(FlowRecord::isFinish),
                        "延迟任务触发后所有记录都应标记为完成"),
                () -> assertEquals(0, flowRecords.stream().filter(FlowRecord::isTodo).count(),
                        "延迟节点流程完成后不应残留待办"));
    }

    /**
     * 测试目标：仅有 A(开始)->B(结束) 的最简流程，提交开始节点后最终流程状态为完成。
     * 前置条件：流程只有开始与结束节点。
     * 执行步骤：提交 A 开始节点。
     * 期望断言：流程全部记录 isFinish()，无残留待办。
     */
    @Test
    void shouldFinishFlowWhenOnlyStartAndEnd() {
        StartNode start = writableStart("简流程开始");
        Workflow workflow = WorkflowBuilder.builder()
                .title("最简自动完成流程")
                .code("auto-complete-simple")
                .createdOperator(initiator)
                .form(form)
                .addNode(start)
                .addNode(EndNode.builder().name("结束").build())
                .build();
        factory.workflowService.saveWorkflow(workflow);

        long startRecordId = createAndSubmit(start, "auto-complete-simple");

        List<FlowRecord> flowRecords = records(factory.flowRecordRepository.get(startRecordId).getProcessId());
        assertAll("最简流程的最终状态",
                () -> assertTrue(flowRecords.stream().allMatch(FlowRecord::isFinish),
                        "最简流程提交后所有记录都应标记为完成"),
                () -> assertEquals(0, flowRecords.stream().filter(FlowRecord::isTodo).count(),
                        "最简流程完成后不应残留待办"));
    }

    /**
     * 测试目标：模拟生产 JPA 仓储对象隔离语义下，主流程 A->SubProcess->B(结束)、
     * 子流程 SA(开始)->SB(抄送)->SC(结束) 全自动完成后的主流程最终状态。
     * <p>
     * 生产仓储每次读写都在领域对象与 JPA 实体间转换：子流程自动完成在嵌套执行中把主流程触发记录
     * 标记为完成，但外层流程动作随后以陈旧的运行中状态再次保存该记录，最终流程状态会被覆盖回「运行中」。
     * 前置条件：使用 copy-on-read 仓储（每次读取返回最近一次保存状态的新对象）。
     * 执行步骤：提交主流程开始节点，整个链路自动流转。
     * 期望断言：主流程全部记录 isFinish()，最终流程状态不得停留在运行中。
     */
    @Test
    void shouldFinishParentFlowWhenSubProcessAutoCompletesWithCopyOnReadRepository() {
        CopyOnReadFlowServiceFactory factory = new CopyOnReadFlowServiceFactory();
        LoopTriggerTraceContext.getInstance().clear();
        User initiator = new User(1, "发起人");
        factory.userGateway.save(initiator);

        StartNode childStart = writableStart("子流程开始");
        NotifyNode childNotify = NotifyNode.builder()
                .name("子流程抄送")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(
                                        "def run(request){ return [" + initiator.getUserId() + "] }").getKey()))
                        .build())
                .build();
        saveChildWorkflow(factory, childStart, childNotify);

        StartNode parentStart = writableStart("主流程开始");
        String createScript = "def run(request){ return request.toCreateRequest('%s', %d, '%s', request.getFormData()) }"
                .formatted(CHILD_CODE, initiator.getUserId(), passAction(childStart).id());
        SubProcessNode subProcess = SubProcessNode.builder()
                .name("子流程节点")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy(
                                FlowGroovyScriptFactory.createSubProcessScript(createScript).getKey(), true))
                        .build())
                .build();
        saveParentWorkflow(factory, parentStart, subProcess);

        long parentRecordId = createAndSubmit(factory, parentStart, initiator, PARENT_CODE);

        FlowRecord parentStartRecord = factory.flowRecordRepository.get(parentRecordId);
        String processId = parentStartRecord.getProcessId();
        List<FlowRecord> parentRecords = factory.flowRecordRepository.findProcessRecords(processId);
        assertAll("copy-on-read 仓储下主流程自动完成后的最终状态",
                () -> assertTrue(parentRecords.stream().allMatch(FlowRecord::isFinish),
                        "主流程自动完成后所有记录都应标记为完成，不能停留在运行中"),
                () -> assertEquals(0, parentRecords.stream().filter(FlowRecord::isTodo).count(),
                        "主流程完成后不应残留待办"));
    }

    private void saveChildWorkflow(StartNode childStart, NotifyNode childNotify) {
        saveChildWorkflow(this.factory, childStart, childNotify);
    }

    private void saveChildWorkflow(MyFlowServiceFactory factory, StartNode childStart, NotifyNode childNotify) {
        WorkflowBuilder builder = WorkflowBuilder.builder()
                .title("子流程自动完成测试")
                .code(CHILD_CODE)
                .createdOperator(initiator)
                .form(form)
                .addNode(childStart);
        if (childNotify != null) {
            builder.addNode(childNotify);
        }
        builder.addNode(EndNode.builder().name("子流程结束").build());
        factory.workflowService.saveWorkflow(builder.build());
    }

    private void saveParentWorkflow(StartNode start, SubProcessNode subProcess) {
        saveParentWorkflow(this.factory, start, subProcess);
    }

    private void saveParentWorkflow(MyFlowServiceFactory factory, StartNode start, SubProcessNode subProcess) {
        Workflow parentWorkflow = WorkflowBuilder.builder()
                .title("主流程自动完成测试")
                .code(PARENT_CODE)
                .createdOperator(initiator)
                .form(form)
                .addNode(start)
                .addNode(subProcess)
                .addNode(EndNode.builder().name("主流程结束").build())
                .build();
        factory.workflowService.saveWorkflow(parentWorkflow);
    }

    private long createAndSubmitParent(StartNode start) {
        return createAndSubmit(this.factory, start, initiator, PARENT_CODE);
    }

    private long createAndSubmit(StartNode start, String workCode) {
        return createAndSubmit(this.factory, start, initiator, workCode);
    }

    private long createAndSubmit(MyFlowServiceFactory factory, StartNode start, User operator, String workCode) {
        Map<String, Object> data = Map.of("content", "auto");
        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode(workCode);
        request.setOperatorId(operator.getUserId());
        request.setActionId(passAction(start).id());
        request.setFormData(data);
        long recordId = factory.flowService.create(request);
        approve(factory, factory.repositoryHolder.getRecordById(recordId), passAction(start), operator, data);
        return recordId;
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

    private void approve(FlowRecord record, IFlowAction action, Map<String, Object> data) {
        approve(this.factory, record, action, initiator, data);
    }

    private void approve(MyFlowServiceFactory factory, FlowRecord record, IFlowAction action,
                         User operator, Map<String, Object> data) {
        FlowActionRequest request = new FlowActionRequest();
        request.setRecordId(record.getId());
        request.setFormData(data);
        request.setAdvice(new FlowAdviceBody(action.id(), "同意", operator.getUserId()));
        factory.flowService.action(request);
    }

    private IFlowAction passAction(IFlowNode node) {
        return node.actionManager().getActions().stream()
                .filter(action -> "PASS".equals(action.type()))
                .findFirst()
                .orElseThrow();
    }

    private List<FlowRecord> records(String processId) {
        return records(this.factory, processId);
    }

    private List<FlowRecord> records(MyFlowServiceFactory factory, String processId) {
        return factory.flowRecordRepository.findProcessRecords(processId);
    }

    private User saveUser(long id, String name) {
        User user = new User(id, name);
        factory.userGateway.save(user);
        return user;
    }
}
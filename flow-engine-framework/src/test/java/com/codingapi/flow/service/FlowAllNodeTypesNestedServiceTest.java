package com.codingapi.flow.service;

import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.domain.DelayTask;
import com.codingapi.flow.domain.DelayTaskManager;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.node.nodes.*;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowProcessNodeRequest;
import com.codingapi.flow.pojo.response.ProcessNode;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.*;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全节点类型复杂流程回归测试。
 *
 * <p>主流程结构：</p>
 * <pre>
 * A
 * └─ Condition                                      第一层
 *    ├─ primary -> Manual                           第二层
 *    │  ├─ primary -> Parallel                      第三层
 *    │  │  ├─ B: B1 -> B2 -> Router -> Trigger -> Notify
 *    │  │  ├─ C: Handle
 *    │  │  └─ D: Delay -> D1
 *    │  └─ alternate -> ManualAlt
 *    └─ else -> ConditionElse
 * -> Inclusive(SubProcess, I2, InclusiveElse) -> E -> F
 * </pre>
 *
 * <p>一个流程定义覆盖 {@link NodeType} 的全部类型；两个运行实例分别覆盖互斥的正常分支和 else 分支。
 * 正常分支在 B2 退回 B1 后重新提交，重点验证重复历史节点、fromId 链和 ProcessNodes 展示顺序。</p>
 */
class FlowAllNodeTypesNestedServiceTest {

    private static final String PARENT_CODE = "all-node-three-level-parent";
    private static final String CHILD_CODE = "all-node-three-level-child";
    private static final String FORM_CODE = "all-node-three-level-form";

    private MyFlowServiceFactory factory;
    private Fixture fixture;

    @BeforeEach
    void setUp() {
        factory = new MyFlowServiceFactory();
        fixture = createFixture();
    }

    @AfterEach
    void tearDown() {
        DelayTaskManager.getInstance().close();
    }

    /**
     * 测试目标：验证单个流程定义包含全部节点类型，且控制节点至少达到三层嵌套。
     * 前置条件：已创建父流程和配套子流程。
     * 执行步骤：递归遍历定义树，并通过 ProcessNodes 接口预览主分支。
     * 期望断言：19 种节点无遗漏、嵌套深度不少于三层、列表展开无重复且保持块内顺序。
     */
    @Test
    void shouldCoverEveryNodeTypeAndFlattenThreeLevelTreeWithoutDuplicates() {
        List<IFlowNode> allNodes = flatten(fixture.parentWorkflow.getNodes());
        EnumSet<NodeType> actualTypes = EnumSet.noneOf(NodeType.class);
        allNodes.forEach(node -> actualTypes.add(NodeType.valueOf(node.getType())));

        List<ProcessNode> preview = factory.flowService.processNodes(
                new FlowProcessNodeRequest(PARENT_CODE, fixture.initiator.getUserId(), mainData()));
        List<String> previewNodeIds = preview.stream().map(ProcessNode::getNodeId).toList();

        assertAll("流程定义和 ProcessNodes 树形展开检查",
                () -> assertEquals(EnumSet.allOf(NodeType.class), actualTypes, "必须覆盖全部流程节点类型"),
                () -> assertTrue(maxControlDepth(fixture.parentWorkflow.getNodes(), 0) >= 3,
                        "Condition -> Manual -> Parallel 必须形成至少三层控制嵌套"),
                () -> assertEquals(previewNodeIds.size(), new HashSet<>(previewNodeIds).size(),
                        "树形节点展平为列表后不得重复展示共享的后续节点"),
                () -> assertInOrder(previewNodeIds,
                        fixture.start, fixture.manual, fixture.b1, fixture.b2, fixture.router,
                        fixture.trigger, fixture.notifyNode, fixture.cHandle, fixture.delay, fixture.d1,
                        fixture.manualAlt, fixture.subProcess, fixture.i2, fixture.finalApproval, fixture.end));
    }

    /**
     * 测试目标：执行三层嵌套主分支，并在 B2 至少退回 B1 一次。
     * 前置条件：主条件与两个包容分支均命中，人工分支选择三路并行块。
     * 执行步骤：手动触发延迟任务；B1 通过、B2 退回、B1/B2 再次通过；完成其余并行、子流程和最终审批。
     * 期望断言：并行正确汇聚，退回历史不被覆盖，fromId 链连续，ProcessNodes 保留重复历史且按块展开，父子流程均结束。
     */
    @Test
    void shouldPreserveReturnHistoryAndCompleteAllActiveNestedBranches() {
        Map<String, Object> data = mainData();
        createAndSubmitParent(data, fixture.primaryManualBranch);

        // Delay 使用一小时配置避免异步竞态，测试中关闭定时器并显式触发。
        List<DelayTask> delayTasks = factory.delayTaskRepository.findAll();
        assertEquals(1, delayTasks.size(), "D 分支应创建一个延迟任务");
        DelayTaskManager.getInstance().close();
        factory.repositoryHolder.createDelayTriggerService(delayTasks.get(0)).trigger();
        factory.repositoryHolder.deleteDelayTask(delayTasks.get(0));

        FlowRecord firstB1 = assertSingleTodo(fixture.b1Operator, fixture.b1);
        FlowRecord cTodo = assertSingleTodo(fixture.cOperator, fixture.cHandle);
        FlowRecord dTodo = assertSingleTodo(fixture.dOperator, fixture.d1);
        String processId = firstB1.getProcessId();

        approve(firstB1, passAction(fixture.b1), fixture.b1Operator, data);
        FlowRecord firstB2 = assertSingleTodo(fixture.b2Operator, fixture.b2);
        returnTo(firstB2, fixture.b2Operator, fixture.b1, data);
        FlowRecord returnedB1 = assertSingleTodo(fixture.b1Operator, fixture.b1);

        List<FlowRecord> recordsAfterReturn = records(processId);
        String parallelIdBeforeQuery = returnedB1.getParallelId();
        assertAll("ProcessNodes 查询前的三路并行元数据",
                () -> assertTrue(List.of(returnedB1, cTodo, dTodo).stream()
                        .allMatch(record -> record.getParallelBranchTotal() == 3)),
                () -> assertTrue(List.of(returnedB1, cTodo, dTodo).stream()
                        .allMatch(record -> parallelIdBeforeQuery.equals(record.getParallelId()))));
        List<ProcessNode> nodesAfterReturn = processNodes(returnedB1, fixture.b1Operator, data);
        List<ProcessNode> historyAfterReturn = recordBackedNodes(recordsAfterReturn, nodesAfterReturn);
        String parallelState = List.of(returnedB1, cTodo, dTodo).stream()
                .map(record -> record.getNodeName() + "=" + record.getParallelId() + "/"
                        + record.getParallelBranchTotal() + "/" + record.getParallelBranchNodeId())
                .toList().toString();

        assertAll("B2 退回 B1 后的历史链和节点记录",
                () -> assertEquals(6, recordsAfterReturn.size(), "应包含 A、B1、B2、退回后的 B1、C、D"),
                () -> assertEquals(3, recordsAfterReturn.stream().filter(FlowRecord::isTodo).count(),
                        "退回后 B1、C、D 三条并行分支仍应待办"),
                () -> assertEquals(firstB1.getId(), firstB2.getFromId(), "第一轮 B2 应来源于第一轮 B1"),
                () -> assertEquals(firstB2.getId(), returnedB1.getFromId(), "退回后的 B1 应来源于执行退回的 B2"),
                () -> assertEquals(ActionType.RETURN.name(), firstB2.getActionType(), "B2 历史动作必须记录为退回"),
                () -> assertTrue(List.of(returnedB1, cTodo, dTodo).stream()
                                .allMatch(record -> record.getParallelBranchTotal() == 3),
                        "退回后各活动分支必须继续保留三路并行总数: " + parallelState),
                () -> assertEquals(1, List.of(returnedB1, cTodo, dTodo).stream()
                                .map(FlowRecord::getParallelId).distinct().count(),
                        "退回后各活动分支必须继续使用同一个 parallelId: " + parallelState),
                () -> assertEquals(List.of("A", "B1", "B2", "B1", "C-Handle", "D1"),
                        historyAfterReturn.stream().map(ProcessNode::getNodeName).toList(),
                        "ProcessNodes 必须保留重复历史，并按 B、C、D 块依次展示"),
                () -> assertEquals(List.of(
                                ProcessNode.ApproveState.PASS,
                                ProcessNode.ApproveState.PASS,
                                ProcessNode.ApproveState.PASS,
                                ProcessNode.ApproveState.PROCESSING,
                                ProcessNode.ApproveState.PROCESSING,
                                ProcessNode.ApproveState.PROCESSING),
                        historyAfterReturn.stream().map(ProcessNode::getApproveState).toList(),
                        "退回动作属于已办历史，新的 B1/C/D 属于处理中"));

        approve(returnedB1, passAction(fixture.b1), fixture.b1Operator, data);
        FlowRecord secondB2 = assertSingleTodo(fixture.b2Operator, fixture.b2);
        assertEquals(returnedB1.getId(), secondB2.getFromId(), "第二轮 B2 必须承接退回后的 B1");

        approve(cTodo, passAction(fixture.cHandle), fixture.cOperator, data);
        assertNoTodo(fixture.i2Operator);
        approve(dTodo, passAction(fixture.d1), fixture.dOperator, data);
        assertNoTodo(fixture.i2Operator);
        approve(secondB2, passAction(fixture.b2), fixture.b2Operator, data);

        FlowRecord i2Todo = assertSingleTodo(fixture.i2Operator, fixture.i2);
        FlowRecord childTodo = assertSingleTodo(fixture.childOperator, fixture.childApproval);
        assertTrue(records(processId).stream().anyMatch(record -> record.getNodeId().equals(fixture.notifyNode.getId())),
                "Router 和 Trigger 执行后必须生成 Notify 历史记录");

        approve(childTodo, passAction(fixture.childApproval), fixture.childOperator, data);
        approve(i2Todo, passAction(fixture.i2), fixture.i2Operator, data);
        FlowRecord finalTodo = assertSingleTodo(fixture.finalOperator, fixture.finalApproval);

        List<FlowRecord> parentRecordsBeforeFinish = records(processId);
        List<ProcessNode> finalNodes = processNodes(finalTodo, fixture.finalOperator, data);
        assertAll("父流程汇聚后的完整节点记录",
                () -> assertEquals(10, parentRecordsBeforeFinish.size(),
                        "父流程应记录 A、两轮 B1/B2、Notify、C、D、I2、E"),
                () -> assertEquals(List.of("A", "B1", "B2", "B1", "B2", "B-Notify",
                                "C-Handle", "D1", "SubProcess", "I2", "E", "F"),
                        finalNodes.stream().map(ProcessNode::getNodeName).toList(),
                        "历史节点保留重复轮次；有执行记录的子流程应展示，未落库控制节点不应伪造历史"),
                () -> assertTrue(finalNodes.stream()
                        .filter(node -> node.getNodeId().equals(fixture.subProcess.getId()))
                        .anyMatch(node -> node.getApproveState() == ProcessNode.ApproveState.PASS
                                && node.getSubProcess() != null
                                && node.getSubProcess().getFinishedCount() == 1),
                        "已完成的子流程节点应展示聚合执行信息"),
                () -> assertEquals(1, parentRecordsBeforeFinish.stream()
                        .filter(record -> record.getNodeId().equals(fixture.notifyNode.getId())).count(),
                        "路由后的抄送记录只能生成一次"),
                () -> assertTrue(factory.delayTaskRepository.findAll().isEmpty(), "延迟任务触发后必须删除"));

        approve(finalTodo, passAction(fixture.finalApproval), fixture.finalOperator, data);
        List<FlowRecord> finishedParentRecords = records(processId);
        List<FlowRecord> childRecords = factory.flowRecordRepository.findDoneByOperator(fixture.childOperator.getUserId());

        assertAll("父子流程完成状态",
                () -> assertTrue(finishedParentRecords.stream().allMatch(FlowRecord::isFinish),
                        "父流程所有记录都应标记完成"),
                () -> assertEquals(0, finishedParentRecords.stream().filter(FlowRecord::isTodo).count(),
                        "父流程完成后不应残留待办"),
                () -> assertEquals(1, childRecords.size(), "子流程审批人应有一条已办"),
                () -> assertTrue(childRecords.get(0).isFinish(), "子流程审批记录应完成"),
                () -> assertTrue(childRecords.get(0).getParentId() > 0, "子流程记录必须关联父记录"),
                () -> assertNotEquals(processId, childRecords.get(0).getProcessId(),
                        "父子流程必须使用不同 processId"));
    }

    /**
     * 测试目标：用第二个实例覆盖 ConditionElseBranch 和 InclusiveElseBranch。
     * 前置条件：主条件不命中，两个包容显式条件均为 false。
     * 执行步骤：依次完成条件 else、包容 else 和最终审批。
     * 期望断言：互斥分支不会产生待办，ProcessNodes 仅呈现实际路径和后续节点，实例正常结束。
     */
    @Test
    void shouldExecuteConditionAndInclusiveElseBranchesAsASeparateInstance() {
        Map<String, Object> data = elseData();
        createAndSubmitParent(data, null);

        FlowRecord conditionElseTodo = assertSingleTodo(fixture.conditionElseOperator, fixture.conditionElseApproval);
        String processId = conditionElseTodo.getProcessId();
        assertNoTodo(fixture.b1Operator);
        approve(conditionElseTodo, passAction(fixture.conditionElseApproval), fixture.conditionElseOperator, data);

        FlowRecord inclusiveElseTodo = assertSingleTodo(fixture.inclusiveElseOperator, fixture.inclusiveElseHandle);
        assertNoTodo(fixture.i2Operator);
        assertNoTodo(fixture.childOperator);
        approve(inclusiveElseTodo, passAction(fixture.inclusiveElseHandle), fixture.inclusiveElseOperator, data);

        FlowRecord finalTodo = assertSingleTodo(fixture.finalOperator, fixture.finalApproval);
        List<ProcessNode> nodes = processNodes(finalTodo, fixture.finalOperator, data);
        assertEquals(List.of("A", "Condition-Else-Approval", "Inclusive-Else-Handle", "E", "F"),
                nodes.stream().map(ProcessNode::getNodeName).toList(),
                "else 实例只能展示实际历史路径及公共后续节点");

        approve(finalTodo, passAction(fixture.finalApproval), fixture.finalOperator, data);
        assertTrue(records(processId).stream().allMatch(FlowRecord::isFinish));
    }

    private Fixture createFixture() {
        User initiator = saveUser(1, "发起人");
        User b1Operator = saveUser(2, "B1审批人");
        User b2Operator = saveUser(3, "B2审批人");
        User cOperator = saveUser(4, "C办理人");
        User notifyOperator = saveUser(5, "B抄送人");
        User dOperator = saveUser(6, "D1审批人");
        User i2Operator = saveUser(7, "I2审批人");
        User finalOperator = saveUser(8, "E审批人");
        User manualAltOperator = saveUser(9, "人工备选审批人");
        User conditionElseOperator = saveUser(10, "条件Else审批人");
        User inclusiveElseOperator = saveUser(11, "包容Else办理人");
        User childOperator = saveUser(20, "子流程审批人");

        FlowForm form = FlowFormBuilder.builder()
                .name("全节点复杂流程表单")
                .code(FORM_CODE)
                .addField("模式", "mode", DataType.STRING)
                .addField("启用子流程", "includeSub", DataType.BOOLEAN)
                .addField("启用审批", "includeApproval", DataType.BOOLEAN)
                .addField("申请内容", "content", DataType.STRING)
                .build();

        StartNode childStart = startNode("Child-A");
        ApprovalNode childApproval = approvalNode("Child-Approval", childOperator);
        EndNode childEnd = EndNode.builder().name("Child-F").build();
        Workflow childWorkflow = WorkflowBuilder.builder()
                .title("全节点复杂流程-子流程")
                .code(CHILD_CODE)
                .createdOperator(initiator)
                .form(form)
                .addNode(childStart)
                .addNode(childApproval)
                .addNode(childEnd)
                .build();
        factory.workflowService.saveWorkflow(childWorkflow);

        StartNode start = startNode("A");
        ApprovalNode b1 = approvalNode("B1", b1Operator);
        ApprovalNode b2 = approvalNode("B2", b2Operator);
        TriggerNode trigger = TriggerNode.builder()
                .name("B-Trigger")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new TriggerStrategy(FlowGroovyScriptFactory
                                .createTriggerScript("def run(request){return null}").getKey()))
                        .build())
                .build();
        RouterNode router = RouterNode.builder()
                .name("B-Router")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new RouterStrategy(FlowGroovyScriptFactory.createRouterScript(
                                "def run(request){return '" + trigger.getId() + "'}").getKey()))
                        .build())
                .build();
        NotifyNode notify = NotifyNode.builder()
                .name("B-Notify")
                .strategies(auditStrategies(notifyOperator, false))
                .build();
        HandleNode cHandle = HandleNode.builder()
                .name("C-Handle")
                .strategies(auditStrategies(cOperator, true))
                .build();
        DelayNode delay = DelayNode.builder()
                .name("D-Delay")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new DelayStrategy(DelayStrategy.Type.HOUR, 1))
                        .build())
                .build();
        ApprovalNode d1 = approvalNode("D1", dOperator);

        ParallelNode parallel = ParallelNode.builder()
                .name("B/C/D-Parallel")
                .blocks(
                        ParallelBranchNode.builder().name("B-Branch").order(1)
                                .blocks(b1, b2, router, trigger, notify).build(),
                        ParallelBranchNode.builder().name("C-Branch").order(2)
                                .blocks(cHandle).build(),
                        ParallelBranchNode.builder().name("D-Branch").order(3)
                                .blocks(delay, d1).build())
                .build();

        ApprovalNode manualAlt = approvalNode("Manual-Alt-Approval", manualAltOperator);
        ManualBranchNode primaryManualBranch = ManualBranchNode.builder()
                .name("Manual-Primary-Branch").order(1).blocks(parallel).build();
        ManualBranchNode alternateManualBranch = ManualBranchNode.builder()
                .name("Manual-Alternate-Branch").order(2).blocks(manualAlt).build();
        ManualNode manual = ManualNode.builder()
                .name("Manual").blocks(primaryManualBranch, alternateManualBranch).build();

        ApprovalNode conditionElseApproval = approvalNode("Condition-Else-Approval", conditionElseOperator);
        ConditionBranchNode primaryConditionBranch = ConditionBranchNode.builder()
                .name("Condition-Primary-Branch").order(1)
                .conditionScript(FlowGroovyScriptFactory.createConditionScript(
                        "def run(request){return request.getFormData('mode') == 'PRIMARY'}").getKey())
                .blocks(manual).build();
        ConditionElseBranchNode conditionElseBranch = ConditionElseBranchNode.builder()
                .name("Condition-Else-Branch").blocks(conditionElseApproval).build();
        ConditionNode condition = ConditionNode.builder()
                .name("Condition").blocks(primaryConditionBranch, conditionElseBranch).build();

        String childScript = "def run(request){return request.toCreateRequest('" + CHILD_CODE + "', "
                + initiator.getUserId() + ", '" + passAction(childStart).id() + "', request.getFormData())}";
        SubProcessNode subProcess = SubProcessNode.builder()
                .name("SubProcess")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy(FlowGroovyScriptFactory
                                .createSubProcessScript(childScript).getKey(), true))
                        .build())
                .build();
        ApprovalNode i2 = approvalNode("I2", i2Operator);
        HandleNode inclusiveElseHandle = HandleNode.builder()
                .name("Inclusive-Else-Handle")
                .strategies(auditStrategies(inclusiveElseOperator, true))
                .build();
        InclusiveBranchNode subProcessBranch = InclusiveBranchNode.builder()
                .name("Inclusive-SubProcess-Branch").order(1)
                .conditionScript(FlowGroovyScriptFactory.createConditionScript(
                        "def run(request){return request.getFormData('includeSub') == true}").getKey())
                .blocks(subProcess).build();
        InclusiveBranchNode approvalBranch = InclusiveBranchNode.builder()
                .name("Inclusive-Approval-Branch").order(2)
                .conditionScript(FlowGroovyScriptFactory.createConditionScript(
                        "def run(request){return request.getFormData('includeApproval') == true}").getKey())
                .blocks(i2).build();
        InclusiveElseBranchNode inclusiveElseBranch = InclusiveElseBranchNode.builder()
                .name("Inclusive-Else-Branch").blocks(inclusiveElseHandle).build();
        InclusiveNode inclusive = InclusiveNode.builder()
                .name("Inclusive").blocks(subProcessBranch, approvalBranch, inclusiveElseBranch).build();

        ApprovalNode finalApproval = approvalNode("E", finalOperator);
        EndNode end = EndNode.builder().name("F").build();
        Workflow parentWorkflow = WorkflowBuilder.builder()
                .title("全节点三层嵌套复杂流程")
                .code(PARENT_CODE)
                .createdOperator(initiator)
                .form(form)
                .addNode(start)
                .addNode(condition)
                .addNode(inclusive)
                .addNode(finalApproval)
                .addNode(end)
                .build();
        factory.workflowService.saveWorkflow(parentWorkflow);

        return new Fixture(parentWorkflow, initiator, start, manual, primaryManualBranch,
                b1, b2, router, trigger, notify, cHandle, delay, d1, manualAlt,
                conditionElseApproval, subProcess, i2, inclusiveElseHandle, finalApproval, end,
                childApproval, b1Operator, b2Operator, cOperator, dOperator, i2Operator,
                finalOperator, conditionElseOperator, inclusiveElseOperator, childOperator);
    }

    private StartNode startNode(String name) {
        return StartNode.builder().name(name)
                .strategies(NodeStrategyBuilder.builder().addStrategy(formPermission(PermissionType.WRITE)).build())
                .build();
    }

    private ApprovalNode approvalNode(String name, User operator) {
        return ApprovalNode.builder().name(name).strategies(auditStrategies(operator, true)).build();
    }

    private List<INodeStrategy> auditStrategies(User operator, boolean resume) {
        NodeStrategyBuilder builder = NodeStrategyBuilder.builder()
                .addStrategy(formPermission(PermissionType.READ))
                .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory.createOperatorLoadScript(
                        "def run(request){return [" + operator.getUserId() + "]}").getKey()));
        if (resume) {
            builder.addStrategy(ResubmitStrategy.defaultStrategy());
        }
        return builder.build();
    }

    private FormFieldPermissionStrategy formPermission(PermissionType type) {
        return new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                .addPermission(FORM_CODE, "mode", type)
                .addPermission(FORM_CODE, "includeSub", type)
                .addPermission(FORM_CODE, "includeApproval", type)
                .addPermission(FORM_CODE, "content", type)
                .build());
    }

    private User saveUser(long id, String name) {
        User user = new User(id, name);
        factory.userGateway.save(user);
        return user;
    }

    private void createAndSubmitParent(Map<String, Object> data, ManualBranchNode selectedBranch) {
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(PARENT_CODE);
        createRequest.setFormData(data);
        createRequest.setActionId(passAction(fixture.start).id());
        createRequest.setOperatorId(fixture.initiator.getUserId());
        long recordId = factory.flowService.create(createRequest);

        FlowAdviceBody advice = new FlowAdviceBody(passAction(fixture.start).id(), "提交", fixture.initiator.getUserId());
        if (selectedBranch != null) {
            advice.setManualNodeId(selectedBranch.getId());
        }
        action(recordId, advice, data);
    }

    private void approve(FlowRecord record, IFlowAction flowAction, User operator, Map<String, Object> data) {
        action(record.getId(), new FlowAdviceBody(flowAction.id(), "同意", operator.getUserId()), data);
    }

    private void returnTo(FlowRecord record, User operator, IFlowNode backNode, Map<String, Object> data) {
        IFlowAction returnAction = recordNode(record).actionManager().getActionByType(ActionType.RETURN.name());
        FlowAdviceBody advice = new FlowAdviceBody(returnAction.id(), "退回重新提交", operator.getUserId());
        advice.setBackNodeId(backNode.getId());
        action(record.getId(), advice, data);
    }

    private IFlowNode recordNode(FlowRecord record) {
        return fixture.parentWorkflow.getFlowNode(record.getNodeId());
    }

    private void action(long recordId, FlowAdviceBody advice, Map<String, Object> data) {
        FlowActionRequest request = new FlowActionRequest();
        request.setRecordId(recordId);
        request.setAdvice(advice);
        request.setFormData(data);
        factory.flowService.action(request);
    }

    private IFlowAction passAction(IFlowNode node) {
        return node.actionManager().getActionByType(ActionType.PASS.name());
    }

    private FlowRecord assertSingleTodo(User user, IFlowNode node) {
        List<FlowRecord> todos = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, todos.size(), user.getName() + " 应有且只有一个待办");
        assertEquals(node.getId(), todos.get(0).getNodeId(), user.getName() + " 的待办节点不正确");
        return todos.get(0);
    }

    private void assertNoTodo(User user) {
        assertEquals(0, factory.flowRecordRepository.findTodoByOperator(user.getUserId()).size(),
                user.getName() + " 不应存在待办");
    }

    private List<FlowRecord> records(String processId) {
        return factory.flowRecordRepository.findProcessRecords(processId).stream()
                .sorted(java.util.Comparator.comparingLong(FlowRecord::getId))
                .toList();
    }

    private List<ProcessNode> processNodes(FlowRecord record, User viewer, Map<String, Object> data) {
        return factory.flowService.processNodes(new FlowProcessNodeRequest(record.getId(), viewer.getUserId(), data));
    }

    private List<ProcessNode> recordBackedNodes(List<FlowRecord> records, List<ProcessNode> nodes) {
        Set<String> recordIds = records.stream().map(record -> String.valueOf(record.getId()))
                .collect(java.util.stream.Collectors.toSet());
        return nodes.stream().filter(node -> recordIds.contains(node.getId())).toList();
    }

    private List<IFlowNode> flatten(List<IFlowNode> roots) {
        List<IFlowNode> nodes = new ArrayList<>();
        if (roots == null) {
            return nodes;
        }
        for (IFlowNode root : roots) {
            nodes.add(root);
            nodes.addAll(flatten(root.blocks()));
        }
        return nodes;
    }

    private int maxControlDepth(List<IFlowNode> nodes, int depth) {
        int max = depth;
        if (nodes == null) {
            return max;
        }
        for (IFlowNode node : nodes) {
            int nextDepth = switch (NodeType.valueOf(node.getType())) {
                case CONDITION, MANUAL, PARALLEL, INCLUSIVE -> depth + 1;
                default -> depth;
            };
            max = Math.max(max, maxControlDepth(node.blocks(), nextDepth));
        }
        return max;
    }

    private void assertInOrder(List<String> actualNodeIds, IFlowNode... expectedNodes) {
        int previous = -1;
        for (IFlowNode node : expectedNodes) {
            int current = actualNodeIds.indexOf(node.getId());
            assertTrue(current > previous, node.getName() + " 应按树形块顺序展开，实际索引="
                    + current + "，前一索引=" + previous + "，全部节点=" + actualNodeIds);
            previous = current;
        }
    }

    private Map<String, Object> mainData() {
        return Map.of("mode", "PRIMARY", "includeSub", true,
                "includeApproval", true, "content", "全节点主分支测试");
    }

    private Map<String, Object> elseData() {
        return Map.of("mode", "OTHER", "includeSub", false,
                "includeApproval", false, "content", "全节点Else分支测试");
    }

    private record Fixture(
            Workflow parentWorkflow,
            User initiator,
            StartNode start,
            ManualNode manual,
            ManualBranchNode primaryManualBranch,
            ApprovalNode b1,
            ApprovalNode b2,
            RouterNode router,
            TriggerNode trigger,
            NotifyNode notifyNode,
            HandleNode cHandle,
            DelayNode delay,
            ApprovalNode d1,
            ApprovalNode manualAlt,
            ApprovalNode conditionElseApproval,
            SubProcessNode subProcess,
            ApprovalNode i2,
            HandleNode inclusiveElseHandle,
            ApprovalNode finalApproval,
            EndNode end,
            ApprovalNode childApproval,
            User b1Operator,
            User b2Operator,
            User cOperator,
            User dOperator,
            User i2Operator,
            User finalOperator,
            User conditionElseOperator,
            User inclusiveElseOperator,
            User childOperator) {
    }
}

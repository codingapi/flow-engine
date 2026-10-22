package com.codingapi.flow.service;

import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.ConditionBranchNode;
import com.codingapi.flow.node.nodes.ConditionElseBranchNode;
import com.codingapi.flow.node.nodes.ConditionNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.RouterNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowProcessNodeRequest;
import com.codingapi.flow.pojo.response.ProcessNode;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.RouterStrategy;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 路由节点摆放位置测试（issue #230）。
 *
 * <p>前端当前只允许把路由节点放在条件分支内部，本测试从后端引擎角度验证
 * 路由节点在更多位置上的实际行为，作为放开前端限制前的可行性依据：</p>
 * <ul>
 *     <li>主流程中（开始节点之后、审批节点之间、结束节点之前）；</li>
 *     <li>条件控制节点之后；</li>
 *     <li>以条件控制节点为跳转目标；</li>
 *     <li>条件分支内部（现有位置，回归验证）与分支首节点。</li>
 * </ul>
 *
 * <p>路由节点是透明的控制节点：自身不产生流程记录，通过脚本返回的目标节点
 * 驱动实际流转，因此记录链（fromId）会直接连接路由前后两个业务节点。</p>
 */
class RouterNodePlacementTest {

    private static final String FORM_CODE = "router-placement-form";

    private MyFlowServiceFactory factory;
    private FlowForm form;
    private User initiator;
    private User approverA;
    private User approverB;
    private User approverC;

    @BeforeEach
    void setUp() {
        factory = new MyFlowServiceFactory();
        initiator = saveUser(1, "发起人");
        approverA = saveUser(2, "审批人A");
        approverB = saveUser(3, "审批人B");
        approverC = saveUser(4, "审批人C");

        form = FlowFormBuilder.builder()
                .name("路由摆放测试表单")
                .code(FORM_CODE)
                .addField("模式", "mode", DataType.STRING)
                .build();
    }

    /**
     * 测试目标：验证路由节点位于主流程中（直接位于开始节点之后），并引导到结构上的下一节点。
     * 前置条件：流程结构为 Start -> Router -> A -> End，路由脚本返回 A。
     * 执行步骤：发起并提交流程，A 审批通过。
     * 期望断言：A 收到待办；路由节点自身不产生流程记录；流程正常结束。
     */
    @Test
    void shouldRouteInMainFlowToStructuralNextNode() {
        Map<String, Object> data = modeA();
        StartNode start = startNode();
        ApprovalNode a = approvalNode("A", approverA);
        EndNode end = endNode();
        RouterNode router = routerNode("Router", a);
        Workflow workflow = saveWorkflow("router-placement-main-next", start, router, a, end);

        long startRecordId = createAndSubmit(workflow, data);

        FlowRecord aTodo = assertSingleTodo(approverA, a);
        String processId = processId(startRecordId);
        assertNoTodo(initiator);
        assertNoRecord(processId, router);
        assertEquals(2, records(processId).size(), "开始节点记录 + A 待办记录");

        approve(aTodo, a, approverA, data);

        List<FlowRecord> finishedRecords = records(processId);
        assertEquals(2, finishedRecords.size(), "路由不产生额外记录");
        assertTrue(finishedRecords.stream().allMatch(FlowRecord::isFinish), "流程结束后全部记录应为完成状态");
        assertEquals(0, finishedRecords.stream().filter(FlowRecord::isTodo).count(), "流程结束后不应残留待办");
    }

    /**
     * 测试目标：验证主流程中的路由节点可以跳过结构上的下一节点，直接引导到更靠后的节点。
     * 前置条件：流程结构为 Start -> Router -> A -> B -> End，路由脚本返回 B。
     * 执行步骤：发起并提交流程，B 审批通过。
     * 期望断言：B 收到待办且来源直接指向开始节点记录；A 从未执行且不产生记录；流程正常结束。
     */
    @Test
    void shouldSkipStructuralNextNodeWhenRouterTargetsLaterNode() {
        Map<String, Object> data = modeA();
        StartNode start = startNode();
        ApprovalNode a = approvalNode("A", approverA);
        ApprovalNode b = approvalNode("B", approverB);
        EndNode end = endNode();
        RouterNode router = routerNode("Router", b);
        Workflow workflow = saveWorkflow("router-placement-main-skip", start, router, a, b, end);

        long startRecordId = createAndSubmit(workflow, data);

        FlowRecord bTodo = assertSingleTodo(approverB, b);
        String processId = processId(startRecordId);
        assertNoTodo(approverA);
        assertEquals(startRecordId, bTodo.getFromId(), "路由透明，B 的来源应直接指向开始节点记录");
        assertNoRecord(processId, a);
        assertNoRecord(processId, router);
        assertEquals(2, records(processId).size(), "开始节点记录 + B 待办记录，A 不应出现");

        approve(bTodo, b, approverB, data);

        List<FlowRecord> finishedRecords = records(processId);
        assertEquals(2, finishedRecords.size());
        assertTrue(finishedRecords.stream().allMatch(FlowRecord::isFinish));
    }

    /**
     * 测试目标：验证路由节点可以位于条件控制节点之后（主流程），承接分支汇聚后的流转。
     * 前置条件：流程结构为 Start -> Condition(A/else B) -> Router -> C -> End，路由脚本返回 C，mode=A。
     * 执行步骤：发起并提交流程，A 审批通过，C 审批通过。
     * 期望断言：A 收到待办、B 不执行；A 通过后由路由节点引导到 C；C 通过后流程结束。
     */
    @Test
    void shouldRouteAfterConditionControlNode() {
        Map<String, Object> data = modeA();
        StartNode start = startNode();
        ApprovalNode a = approvalNode("A", approverA);
        ApprovalNode b = approvalNode("B", approverB);
        ApprovalNode c = approvalNode("C", approverC);
        RouterNode router = routerNode("Router", c);
        ConditionNode condition = conditionNode(
                conditionBranch("分支A", 1, "A", a),
                conditionElseBranch("默认分支", b));
        EndNode end = endNode();
        Workflow workflow = saveWorkflow("router-placement-after-condition", start, condition, router, c, end);

        long startRecordId = createAndSubmit(workflow, data);

        FlowRecord aTodo = assertSingleTodo(approverA, a);
        assertNoTodo(approverB);
        assertNoTodo(approverC);

        approve(aTodo, a, approverA, data);

        FlowRecord cTodo = assertSingleTodo(approverC, c);
        String processId = processId(startRecordId);
        assertNoTodo(approverB);
        assertNoRecord(processId, b);
        assertEquals(aTodo.getId(), cTodo.getFromId(), "分支汇聚后应由路由节点直接衔接 C");

        approve(cTodo, c, approverC, data);

        List<FlowRecord> finishedRecords = records(processId);
        assertEquals(3, finishedRecords.size(), "开始节点、A、C 三条记录");
        assertTrue(finishedRecords.stream().allMatch(FlowRecord::isFinish));
        assertNoRecord(processId, router);
    }

    /**
     * 测试目标：验证路由节点可以跳转到条件控制节点，由条件节点完成正常的分支选择。
     * 前置条件：流程结构为 Start -> Router -> C -> Condition(A/else B) -> End，路由脚本返回条件节点，mode=A。
     * 执行步骤：发起并提交流程，A 审批通过。
     * 期望断言：路由跳过结构上的下一节点 C；条件节点选择 A 分支生成待办；C 不产生记录；流程结束。
     */
    @Test
    void shouldRouteToConditionControlNode() {
        Map<String, Object> data = modeA();
        StartNode start = startNode();
        ApprovalNode a = approvalNode("A", approverA);
        ApprovalNode b = approvalNode("B", approverB);
        ApprovalNode c = approvalNode("C", approverC);
        ConditionNode condition = conditionNode(
                conditionBranch("分支A", 1, "A", a),
                conditionElseBranch("默认分支", b));
        RouterNode router = routerNode("Router", condition);
        EndNode end = endNode();
        Workflow workflow = saveWorkflow("router-placement-to-condition", start, router, c, condition, end);

        long startRecordId = createAndSubmit(workflow, data);

        FlowRecord aTodo = assertSingleTodo(approverA, a);
        String processId = processId(startRecordId);
        assertNoTodo(approverB);
        assertNoTodo(approverC);
        assertNoRecord(processId, c);
        assertEquals(2, records(processId).size(), "开始节点记录 + A 待办记录，C 不应出现");

        approve(aTodo, a, approverA, data);

        List<FlowRecord> finishedRecords = records(processId);
        assertEquals(2, finishedRecords.size());
        assertTrue(finishedRecords.stream().allMatch(FlowRecord::isFinish));
    }

    /**
     * 测试目标：验证路由节点可以直接跳转到结束节点，提前结束流程。
     * 前置条件：流程结构为 Start -> Router -> A -> End，路由脚本返回结束节点。
     * 执行步骤：发起并提交流程。
     * 期望断言：A 不产生待办和记录；开始节点记录直接完成；流程结束。
     */
    @Test
    void shouldRouteToEndNode() {
        Map<String, Object> data = modeA();
        StartNode start = startNode();
        ApprovalNode a = approvalNode("A", approverA);
        EndNode end = endNode();
        RouterNode router = routerNode("Router", end);
        Workflow workflow = saveWorkflow("router-placement-to-end", start, router, a, end);

        long startRecordId = createAndSubmit(workflow, data);

        String processId = processId(startRecordId);
        List<FlowRecord> finishedRecords = records(processId);
        assertEquals(1, finishedRecords.size(), "只有开始节点记录，A 不应执行");
        FlowRecord startRecord = finishedRecords.get(0);
        assertTrue(startRecord.isFinish(), "跳转结束节点后流程记录应直接完成");
        assertFalse(startRecord.isTodo(), "流程结束后开始节点不应残留待办");
        assertNoTodo(approverA);
        assertNoRecord(processId, a);
        assertNoRecord(processId, router);
    }

    /**
     * 测试目标：回归验证路由节点位于条件分支内部并跳转到其他分支（前端当前唯一允许的位置）。
     * 前置条件：流程结构为 Start -> Condition(branchA: A -> Router; else: B) -> End，路由脚本返回 B，mode=A。
     * 执行步骤：发起并提交流程，A 审批通过，B 审批通过。
     * 期望断言：A、B 依次待办；B 的来源指向 A 的记录；路由节点无记录；流程正常结束。
     */
    @Test
    void shouldRouteFromConditionBranchToSiblingBranch() {
        Map<String, Object> data = modeA();
        StartNode start = startNode();
        ApprovalNode a = approvalNode("A", approverA);
        ApprovalNode b = approvalNode("B", approverB);
        RouterNode router = routerNode("Router", b);
        ConditionBranchNode branchA = ConditionBranchNode.builder()
                .name("分支A")
                .order(1)
                .conditionScript(conditionScript("A"))
                .blocks(a, router)
                .build();
        ConditionNode condition = conditionNode(branchA, conditionElseBranch("默认分支", b));
        EndNode end = endNode();
        Workflow workflow = saveWorkflow("router-placement-in-branch", start, condition, end);

        long startRecordId = createAndSubmit(workflow, data);

        FlowRecord aTodo = assertSingleTodo(approverA, a);
        assertNoTodo(approverB);

        approve(aTodo, a, approverA, data);

        FlowRecord bTodo = assertSingleTodo(approverB, b);
        String processId = processId(startRecordId);
        assertEquals(aTodo.getId(), bTodo.getFromId(), "跨分支路由后 B 的来源应指向 A 的记录");
        assertNoRecord(processId, router);

        approve(bTodo, b, approverB, data);

        List<FlowRecord> finishedRecords = records(processId);
        assertEquals(3, finishedRecords.size(), "开始节点、A、B 三条记录");
        assertTrue(finishedRecords.stream().allMatch(FlowRecord::isFinish));
    }

    /**
     * 测试目标：验证路由节点位于两个审批节点之间时的正常流转与记录链。
     * 前置条件：流程结构为 Start -> A -> Router -> B -> End，路由脚本返回 B。
     * 执行步骤：发起并提交流程，A 审批通过，B 审批通过。
     * 期望断言：B 待办的来源指向 A 的记录；路由节点无记录；流程正常结束。
     */
    @Test
    void shouldRouteBetweenApprovalsAndKeepFromIdChain() {
        Map<String, Object> data = modeA();
        StartNode start = startNode();
        ApprovalNode a = approvalNode("A", approverA);
        ApprovalNode b = approvalNode("B", approverB);
        EndNode end = endNode();
        RouterNode router = routerNode("Router", b);
        Workflow workflow = saveWorkflow("router-placement-between-approvals", start, a, router, b, end);

        long startRecordId = createAndSubmit(workflow, data);

        FlowRecord aTodo = assertSingleTodo(approverA, a);

        approve(aTodo, a, approverA, data);

        FlowRecord bTodo = assertSingleTodo(approverB, b);
        String processId = processId(startRecordId);
        assertEquals(aTodo.getId(), bTodo.getFromId(), "路由透明，B 的来源应指向 A 的记录");
        assertNoRecord(processId, router);

        approve(bTodo, b, approverB, data);

        List<FlowRecord> finishedRecords = records(processId);
        assertEquals(3, finishedRecords.size(), "开始节点、A、B 三条记录");
        assertTrue(finishedRecords.stream().allMatch(FlowRecord::isFinish));
    }

    /**
     * 测试目标：验证流程预览可以正确展示位于主流程中的路由节点及其动态目标。
     * 前置条件：流程结构为 Start -> Router -> A -> End，路由脚本返回 A。
     * 执行步骤：通过 processNodes 以流程编码预览流程节点。
     * 期望断言：预览顺序为 Start、Router、A、End；路由节点类型为 ROUTER。
     */
    @Test
    void shouldPreviewRouterInMainFlow() {
        Map<String, Object> data = modeA();
        StartNode start = startNode();
        ApprovalNode a = approvalNode("A", approverA);
        EndNode end = endNode();
        RouterNode router = routerNode("Router", a);
        Workflow workflow = saveWorkflow("router-placement-preview", start, router, a, end);

        List<ProcessNode> previewNodes = factory.flowService.processNodes(
                new FlowProcessNodeRequest(workflow.getCode(), initiator.getUserId(), data));

        assertEquals(List.of(start.getId(), router.getId(), a.getId(), end.getId()),
                previewNodes.stream().map(ProcessNode::getNodeId).toList(),
                "预览应按定义顺序展示开始节点、路由节点、路由目标和结束节点");
        assertEquals(RouterNode.NODE_TYPE, previewNodes.get(1).getNodeType(), "第二个节点应为路由节点");
        assertEquals(a.getId(), previewNodes.get(2).getNodeId(), "路由节点之后应展示其脚本目标节点");
    }

    /**
     * 测试目标：验证路由节点作为条件分支首节点时，可以跳出分支块进入公共后续节点。
     * 前置条件：流程结构为 Start -> Condition(branchA: Router; else: B) -> C -> End，路由脚本返回 C，mode=A。
     * 执行步骤：发起并提交流程，C 审批通过。
     * 期望断言：C 收到待办、B 不执行；C 通过后流程结束。
     */
    @Test
    void shouldRouteOutOfBranchWhenRouterIsBranchEntryNode() {
        Map<String, Object> data = modeA();
        StartNode start = startNode();
        ApprovalNode b = approvalNode("B", approverB);
        ApprovalNode c = approvalNode("C", approverC);
        RouterNode router = routerNode("Router", c);
        ConditionBranchNode branchA = ConditionBranchNode.builder()
                .name("分支A")
                .order(1)
                .conditionScript(conditionScript("A"))
                .blocks(router)
                .build();
        ConditionNode condition = conditionNode(branchA, conditionElseBranch("默认分支", b));
        EndNode end = endNode();
        Workflow workflow = saveWorkflow("router-placement-branch-entry", start, condition, c, end);

        long startRecordId = createAndSubmit(workflow, data);

        FlowRecord cTodo = assertSingleTodo(approverC, c);
        String processId = processId(startRecordId);
        assertNoTodo(approverB);
        assertNoRecord(processId, b);

        approve(cTodo, c, approverC, data);

        List<FlowRecord> finishedRecords = records(processId);
        assertEquals(2, finishedRecords.size(), "开始节点、C 两条记录");
        assertTrue(finishedRecords.stream().allMatch(FlowRecord::isFinish));
    }

    private User saveUser(long id, String name) {
        User user = new User(id, name);
        factory.userGateway.save(user);
        return user;
    }

    private Map<String, Object> modeA() {
        return Map.of("mode", "A");
    }

    private StartNode startNode() {
        return StartNode.builder()
                .name("开始")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(formPermission(PermissionType.WRITE))
                        .build())
                .build();
    }

    private ApprovalNode approvalNode(String name, User operator) {
        return ApprovalNode.builder()
                .name(name)
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(formPermission(PermissionType.READ))
                        .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory.createOperatorLoadScript(
                                "def run(request){return [" + operator.getUserId() + "]}").getKey()))
                        .build())
                .build();
    }

    private RouterNode routerNode(String name, IFlowNode target) {
        return RouterNode.builder()
                .name(name)
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new RouterStrategy(FlowGroovyScriptFactory.createRouterScript(
                                "def run(request){return '" + target.getId() + "'}").getKey()))
                        .build())
                .build();
    }

    private ConditionBranchNode conditionBranch(String name, int order, String mode, IFlowNode... blocks) {
        return ConditionBranchNode.builder()
                .name(name)
                .order(order)
                .conditionScript(conditionScript(mode))
                .blocks(blocks)
                .build();
    }

    private ConditionElseBranchNode conditionElseBranch(String name, IFlowNode... blocks) {
        return ConditionElseBranchNode.builder()
                .name(name)
                .blocks(blocks)
                .build();
    }

    private ConditionNode conditionNode(IFlowNode... branches) {
        return ConditionNode.builder()
                .name("条件节点")
                .blocks(branches)
                .build();
    }

    private String conditionScript(String mode) {
        return FlowGroovyScriptFactory.createConditionScript(
                "def run(request){return request.getFormData('mode') == '" + mode + "'}").getKey();
    }

    private EndNode endNode() {
        return EndNode.builder().name("结束").build();
    }

    private FormFieldPermissionStrategy formPermission(PermissionType type) {
        return new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                .addPermission(FORM_CODE, "mode", type)
                .build());
    }

    private Workflow saveWorkflow(String code, IFlowNode... nodes) {
        WorkflowBuilder builder = WorkflowBuilder.builder()
                .title(code)
                .code(code)
                .createdOperator(initiator)
                .form(form);
        for (IFlowNode node : nodes) {
            builder.addNode(node);
        }
        Workflow workflow = builder.build();
        factory.workflowService.saveWorkflow(workflow);
        return workflow;
    }

    private long createAndSubmit(Workflow workflow, Map<String, Object> data) {
        IFlowNode startNode = workflow.getStartNode();
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(passAction(startNode).id());
        createRequest.setOperatorId(initiator.getUserId());
        long recordId = factory.flowService.create(createRequest);

        action(recordId, new FlowAdviceBody(passAction(startNode).id(), "提交", initiator.getUserId()), data);
        return recordId;
    }

    private void approve(FlowRecord record, IFlowNode node, User operator, Map<String, Object> data) {
        action(record.getId(), new FlowAdviceBody(passAction(node).id(), "同意", operator.getUserId()), data);
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

    private String processId(long startRecordId) {
        return factory.flowRecordRepository.get(startRecordId).getProcessId();
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

    private void assertNoRecord(String processId, IFlowNode node) {
        assertTrue(records(processId).stream().noneMatch(record -> record.getNodeId().equals(node.getId())),
                node.getName() + " 不应产生流程记录");
    }

    private List<FlowRecord> records(String processId) {
        return factory.flowRecordRepository.findProcessRecords(processId).stream()
                .sorted(Comparator.comparingLong(FlowRecord::getId))
                .toList();
    }
}
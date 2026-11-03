package com.codingapi.flow.service;

import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.builder.ActionBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowProcessNodeRequest;
import com.codingapi.flow.pojo.response.ProcessNode;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.MultiOperatorAuditStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * issue #239 扩展覆盖：转办 / 委派与四种多人审批模式的组合场景。
 *
 * <p>四种模式（{@link MultiOperatorAuditStrategy.Type}）：
 * <ul>
 *     <li>SEQUENCE：依次审批，后序人员先隐藏；</li>
 *     <li>MERGE：合并审核（此处比例 1.0，全部审批人通过节点才完成）；</li>
 *     <li>ANY：任意审核（或签），一人通过节点即完成；</li>
 *     <li>RANDOM_ONE：随机一人审批，仅产生一条记录。</li>
 * </ul>
 *
 * <p>流程设计：A(开始) -> B(审批，b1/b2，多模式) -> C(审批，c1) -> D(结束)。
 *
 * <p>统一不变量（每个场景结束后校验）：
 * <ol>
 *     <li>B 节点不得残留任何待办；</li>
 *     <li>流程只能出现一条 C 节点待办，不得出现 B、C 同时可审批；</li>
 *     <li>B 节点记录全部为已办，且节点视图不再展示为审批中。</li>
 * </ol>
 */
class FlowIssue239TransferDelegateModeTest {

    private MyFlowServiceFactory factory;
    private User a;
    private User b1;
    private User b2;
    private User c1;
    private User transferTarget;
    private User delegateTarget;

    private StartNode startNode;
    private ApprovalNode bNode;
    private ApprovalNode cNode;
    private EndNode endNode;
    private Workflow workflow;
    private Map<String, Object> data;

    @BeforeEach
    void setUp() {
        factory = new MyFlowServiceFactory();
        a = new User(1, "a");
        b1 = new User(2, "b1");
        b2 = new User(3, "b2");
        c1 = new User(4, "c1");
        transferTarget = new User(5, "b3");
        delegateTarget = new User(6, "d1");
        registerUsers(a, b1, b2, c1, transferTarget, delegateTarget);
    }

    // ==================== 转办 × 四种审批模式 ====================

    /**
     * SEQUENCE：b1 转办给 b3 后，b3 占用 b1 的审批槽位：
     * b3 通过后 b2 才被激活，b2 通过后流程才进入 C。
     */
    @Test
    void sequenceTransferShouldKeepSlotAndActivateNextOperator() {
        prepareWorkflow(MultiOperatorAuditStrategy.Type.SEQUENCE);
        FlowRecord b1Todo = todoOf(b1);
        assertNoTodo(b2);

        transfer(b1Todo, b1, transferTarget);

        assertNoTodo(b1);
        assertNoTodo(b2);
        FlowRecord b3Todo = todoOf(transferTarget);
        assertEquals(bNode.getId(), b3Todo.getNodeId());

        pass(b3Todo, transferTarget);
        FlowRecord b2Todo = todoOf(b2);
        assertNoTodo(c1);

        pass(b2Todo, b2);
        FlowRecord c1Todo = todoOf(c1);
        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(c1.getUserId()).size());
        assertFlowArrivedOnlyAt(c1Todo, bNode);
        assertNoTodoAtNode(c1Todo, bNode);
        assertEquals(3, recordsOfNode(processRecords(c1Todo), bNode).size(),
                "B 节点应保留 b1、b2、b3 三条记录");
    }

    /**
     * ANY：b1 转办给 b3 后，b3 任一人通过即完成 B 节点，b2 的待办自动办结。
     */
    @Test
    void anyTransferShouldAutoDoneSiblingAndAdvance() {
        prepareWorkflow(MultiOperatorAuditStrategy.Type.ANY);
        FlowRecord b1Todo = todoOf(b1);
        todoOf(b2);

        transfer(b1Todo, b1, transferTarget);

        assertNoTodo(b1);
        todoOf(b2);
        FlowRecord b3Todo = todoOf(transferTarget);

        pass(b3Todo, transferTarget);
        assertNoTodo(b2);

        FlowRecord c1Todo = todoOf(c1);
        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(c1.getUserId()).size());
        assertFlowArrivedOnlyAt(c1Todo, bNode);
        assertNoTodoAtNode(c1Todo, bNode);
        assertEquals(3, recordsOfNode(processRecords(c1Todo), bNode).size());
    }

    /**
     * MERGE(1.0)：转办不新增审批席位，b3 通过只代表原席位完成，仍需等待 b2；
     * b2 通过后流程才进入 C。
     */
    @Test
    void mergeTransferShouldNotCountHandoffAsApproval() {
        prepareWorkflow(MultiOperatorAuditStrategy.Type.MERGE);
        FlowRecord b1Todo = todoOf(b1);
        todoOf(b2);

        transfer(b1Todo, b1, transferTarget);

        pass(todoOf(transferTarget), transferTarget);
        assertNoTodo(c1);
        FlowRecord b2Todo = todoOf(b2);

        pass(b2Todo, b2);
        FlowRecord c1Todo = todoOf(c1);
        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(c1.getUserId()).size());
        assertFlowArrivedOnlyAt(c1Todo, bNode);
        assertNoTodoAtNode(c1Todo, bNode);
        assertEquals(3, recordsOfNode(processRecords(c1Todo), bNode).size());
    }

    /**
     * RANDOM_ONE：仅被随机选中的人可转办；转办记录通过后流程直接进入 C。
     */
    @Test
    void randomOneTransferShouldAdvanceWithoutLeftover() {
        prepareWorkflow(MultiOperatorAuditStrategy.Type.RANDOM_ONE);
        User active = activeInitialApprover();
        User other = active == b1 ? b2 : b1;
        FlowRecord activeTodo = todoOf(active);
        assertNoTodo(other);
        assertEquals(1, recordsOfNode(processRecords(activeTodo), bNode).size(),
                "随机一人模式初始只应产生一条 B 节点记录");

        transfer(activeTodo, active, transferTarget);

        assertNoTodo(active);
        pass(todoOf(transferTarget), transferTarget);

        FlowRecord c1Todo = todoOf(c1);
        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(c1.getUserId()).size());
        assertFlowArrivedOnlyAt(c1Todo, bNode);
        assertNoTodoAtNode(c1Todo, bNode);
        assertEquals(2, recordsOfNode(processRecords(c1Todo), bNode).size(),
                "B 节点应保留原审批人与转办目标两条记录");
    }

    /**
     * 转办给多人：任一人通过后，其余转办目标与其他同节点待办都应自动办结。
     */
    @Test
    void anyTransferToMultipleTargetsShouldCloseAllSiblings() {
        User target2 = new User(7, "b4");
        registerUsers(target2);
        prepareWorkflow(MultiOperatorAuditStrategy.Type.ANY);
        FlowRecord b1Todo = todoOf(b1);
        todoOf(b2);

        transfer(b1Todo, b1, List.of(transferTarget.getUserId(), target2.getUserId()));
        todoOf(b2);
        todoOf(transferTarget);
        todoOf(target2);

        pass(todoOf(transferTarget), transferTarget);

        assertNoTodo(b2);
        assertNoTodo(target2);
        FlowRecord c1Todo = todoOf(c1);
        assertFlowArrivedOnlyAt(c1Todo, bNode);
        assertNoTodoAtNode(c1Todo, bNode);
        assertEquals(4, recordsOfNode(processRecords(c1Todo), bNode).size());
    }

    /**
     * 历史数据兼容：升级前转办记录的 fromId 指向同节点原记录，
     * 办理时仍应按原记录的分组归并，不能留下 b2 的待办。
     */
    @Test
    void legacyTransferRecordShouldStillJoinNodeInstance() {
        prepareWorkflow(MultiOperatorAuditStrategy.Type.ANY);
        FlowRecord b1Todo = todoOf(b1);
        todoOf(b2);

        transfer(b1Todo, b1, transferTarget);
        FlowRecord b3Todo = todoOf(transferTarget);
        // 模拟修复前的数据：转办记录 fromId 指向被转办的 b1 记录，而不是节点实例分组 id
        b3Todo.setFromId(b1Todo.getId());

        pass(b3Todo, transferTarget);

        assertNoTodo(b2);
        FlowRecord c1Todo = todoOf(c1);
        assertFlowArrivedOnlyAt(c1Todo, bNode);
        assertNoTodoAtNode(c1Todo, bNode);
    }

    // ==================== 委派 × 四种审批模式 ====================

    /**
     * SEQUENCE：委派记录通过后必须返回原审批人确认，且不得提前激活下一位审批人。
     */
    @Test
    void sequenceDelegateShouldReturnToOriginalBeforeActivatingNext() {
        prepareWorkflow(MultiOperatorAuditStrategy.Type.SEQUENCE);
        FlowRecord b1Todo = todoOf(b1);

        delegate(b1Todo, b1, delegateTarget);
        assertNoTodo(b1);
        assertNoTodo(b2);
        FlowRecord delegateTodo = todoOf(delegateTarget);

        pass(delegateTodo, delegateTarget);
        // 节点未完成：b2 不能被提前激活，流程也不能进入 C
        assertNoTodo(b2);
        assertNoTodo(c1);
        FlowRecord rebackTodo = todoOf(b1);
        assertEquals(bNode.getId(), rebackTodo.getNodeId());

        pass(rebackTodo, b1);
        FlowRecord b2Todo = todoOf(b2);
        assertNoTodo(c1);

        pass(b2Todo, b2);
        FlowRecord c1Todo = todoOf(c1);
        assertFlowArrivedOnlyAt(c1Todo, bNode);
        assertNoTodoAtNode(c1Todo, bNode);
        assertEquals(4, recordsOfNode(processRecords(c1Todo), bNode).size(),
                "B 节点应保留 b1、委派人、返回记录、b2 四条记录");
    }

    /**
     * ANY：委派记录通过后交还原审批人；原审批人确认后其他同节点待办自动办结。
     */
    @Test
    void anyDelegateShouldReturnToOriginalThenAutoDoneSibling() {
        prepareWorkflow(MultiOperatorAuditStrategy.Type.ANY);
        FlowRecord b1Todo = todoOf(b1);
        todoOf(b2);

        delegate(b1Todo, b1, delegateTarget);
        todoOf(b2);

        pass(todoOf(delegateTarget), delegateTarget);
        // 委派通过但节点尚未最终完成，不能进入 C，也不能提前办结 b2
        assertNoTodo(c1);
        assertFalse(factory.flowRecordRepository.findTodoByOperator(b2.getUserId()).isEmpty(),
                "委派通过后原审批人尚未确认，b2 的待办不应被提前办结");
        FlowRecord rebackTodo = todoOf(b1);

        pass(rebackTodo, b1);
        assertNoTodo(b2);
        FlowRecord c1Todo = todoOf(c1);
        assertFlowArrivedOnlyAt(c1Todo, bNode);
        assertNoTodoAtNode(c1Todo, bNode);
    }

    /**
     * MERGE(1.0)：委派替办记录不计入审批比例，节点完成仍需原审批人确认与 b2 通过。
     */
    @Test
    void mergeDelegateShouldNotCountDelegateAsApproval() {
        prepareWorkflow(MultiOperatorAuditStrategy.Type.MERGE);
        FlowRecord b1Todo = todoOf(b1);
        todoOf(b2);

        delegate(b1Todo, b1, delegateTarget);

        pass(todoOf(delegateTarget), delegateTarget);
        assertNoTodo(c1);
        todoOf(b2);
        FlowRecord rebackTodo = todoOf(b1);

        pass(rebackTodo, b1);
        assertNoTodo(c1);
        FlowRecord b2Todo = todoOf(b2);

        pass(b2Todo, b2);
        FlowRecord c1Todo = todoOf(c1);
        assertFlowArrivedOnlyAt(c1Todo, bNode);
        assertNoTodoAtNode(c1Todo, bNode);
    }

    /**
     * RANDOM_ONE：委派通过后返回原审批人，原审批人确认后流程进入 C。
     */
    @Test
    void randomOneDelegateShouldReturnThenAdvance() {
        prepareWorkflow(MultiOperatorAuditStrategy.Type.RANDOM_ONE);
        User active = activeInitialApprover();
        User other = active == b1 ? b2 : b1;
        FlowRecord activeTodo = todoOf(active);

        delegate(activeTodo, active, delegateTarget);
        assertNoTodo(active);
        assertNoTodo(other);

        pass(todoOf(delegateTarget), delegateTarget);
        assertNoTodo(c1);
        FlowRecord rebackTodo = todoOf(active);

        pass(rebackTodo, active);
        FlowRecord c1Todo = todoOf(c1);
        assertFlowArrivedOnlyAt(c1Todo, bNode);
        assertNoTodoAtNode(c1Todo, bNode);
    }

    /**
     * ANY：委派期间同节点其他人先通过，节点按或签规则完成；
     * 委派记录自动办结，流程正常进入 C，不得出现两个节点同时可审批。
     */
    @Test
    void anySiblingPassDuringDelegateShouldCompleteNode() {
        prepareWorkflow(MultiOperatorAuditStrategy.Type.ANY);
        FlowRecord b1Todo = todoOf(b1);
        FlowRecord b2Todo = todoOf(b2);

        delegate(b1Todo, b1, delegateTarget);
        FlowRecord delegateTodo = todoOf(delegateTarget);

        pass(b2Todo, b2);

        assertNoTodo(delegateTarget);
        assertTrue(factory.flowRecordRepository.get(delegateTodo.getId()).isAutoDone(),
                "节点完成后，委派记录应自动办结");
        FlowRecord c1Todo = todoOf(c1);
        assertFlowArrivedOnlyAt(c1Todo, bNode);
        assertNoTodoAtNode(c1Todo, bNode);
    }

    // ==================== 流程装配 ====================

    private void registerUsers(User... users) {
        for (User user : users) {
            factory.userGateway.save(user);
        }
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);
    }

    private void prepareWorkflow(MultiOperatorAuditStrategy.Type type) {
        float percent = type == MultiOperatorAuditStrategy.Type.MERGE ? 1.0f : 0;
        startNode = StartNode.builder()
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();
        bNode = approvalNode("B审批", "[2,3]", new MultiOperatorAuditStrategy(type, percent));
        cNode = approvalNode("C审批", "[4]", null);
        endNode = EndNode.builder().build();

        workflow = WorkflowBuilder.builder()
                .title("issue239")
                .code("issue239")
                .createdOperator(a)
                .form(form())
                .addNode(startNode)
                .addNode(bNode)
                .addNode(cNode)
                .addNode(endNode)
                .build();
        factory.workflowService.saveWorkflow(workflow);

        data = Map.of("name", "lorne");
        startAndSubmit();
    }

    private FlowForm form() {
        return FlowFormBuilder.builder()
                .name("issue239")
                .code("issue239")
                .addField("name", "name", DataType.STRING)
                .build();
    }

    private ApprovalNode approvalNode(String name, String operatorIds,
                                      MultiOperatorAuditStrategy auditStrategy) {
        NodeStrategyBuilder strategyBuilder = NodeStrategyBuilder.builder()
                .addStrategy(new OperatorLoadStrategy(
                        FlowGroovyScriptFactory.createOperatorLoadScript(
                                "def run(request){return " + operatorIds + "}").getKey()));
        if (auditStrategy != null) {
            strategyBuilder.addStrategy(auditStrategy);
        }
        return ApprovalNode.builder()
                .name(name)
                .strategies(strategyBuilder.build())
                .build();
    }

    private void startAndSubmit() {
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startNode.actionManager().getFirstAction().id());
        createRequest.setOperatorId(a.getUserId());
        factory.flowService.create(createRequest);

        pass(todoOf(a), startNode, a);
    }

    // ==================== 操作 ====================

    private void pass(FlowRecord record, IFlowNode node, User operator) {
        flowAction(record, action(node, ActionType.PASS), operator, null);
    }

    private void pass(FlowRecord record, User operator) {
        pass(record, bNode, operator);
    }

    private void transfer(FlowRecord record, User operator, User target) {
        transfer(record, operator, List.of(target.getUserId()));
    }

    private void transfer(FlowRecord record, User operator, List<Long> targetIds) {
        flowAction(record, action(bNode, ActionType.TRANSFER), operator, targetIds);
    }

    private void delegate(FlowRecord record, User operator, User target) {
        flowAction(record, action(bNode, ActionType.DELEGATE), operator,
                List.of(target.getUserId()));
    }

    private IFlowAction action(IFlowNode node, ActionType type) {
        IFlowAction action = node.actionManager().getActionByType(type.name());
        assertTrue(action != null, "节点[" + node.getName() + "]缺少动作：" + type);
        return action;
    }

    private void flowAction(FlowRecord record, IFlowAction action, User operator,
                            List<Long> forwardOperatorIds) {
        FlowActionRequest request = new FlowActionRequest();
        request.setFormData(data);
        request.setRecordId(record.getId());
        FlowAdviceBody advice = new FlowAdviceBody(action.id(), action.title(), operator.getUserId());
        advice.setForwardOperatorIds(forwardOperatorIds);
        request.setAdvice(advice);
        factory.flowService.action(request);
    }

    // ==================== 断言 ====================

    private User activeInitialApprover() {
        if (!factory.flowRecordRepository.findTodoByOperator(b1.getUserId()).isEmpty()) {
            return b1;
        }
        return b2;
    }

    private FlowRecord todoOf(User user) {
        List<FlowRecord> list = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, list.size(), user.getName() + " 应有且仅有一条待办");
        return list.get(0);
    }

    private void assertNoTodo(User user) {
        assertEquals(0, factory.flowRecordRepository.findTodoByOperator(user.getUserId()).size(),
                user.getName() + " 不应有待办");
    }

    private void assertNoTodoAtNode(FlowRecord anyRecord, IFlowNode node) {
        List<FlowRecord> todos = factory.flowRecordRepository.findTodoRecords(anyRecord.getProcessId());
        assertTrue(todos.stream().noneMatch(record -> node.getId().equals(record.getNodeId())),
                node.getName() + " 不应残留待办");
    }

    /**
     * 校验流程只到达 C 节点：C 节点唯一待办处于审批中，B 节点不再审批中。
     */
    private void assertFlowArrivedOnlyAt(FlowRecord cTodo, IFlowNode bNode) {
        assertEquals(cNode.getId(), cTodo.getNodeId());
        List<ProcessNode> nodeList = factory.flowService.processNodes(
                new FlowProcessNodeRequest(cTodo.getId(), a.getUserId(), data));
        assertTrue(nodeList.stream()
                        .filter(node -> bNode.getId().equals(node.getNodeId()))
                        .noneMatch(node -> node.getApproveState() == ProcessNode.ApproveState.PROCESSING),
                "B 节点不应同时展示为审批中");
        ProcessNode cView = nodeList.stream()
                .filter(node -> cNode.getId().equals(node.getNodeId()))
                .filter(node -> !node.isHistory())
                .findFirst()
                .orElseThrow(() -> new AssertionError("流程视图中缺少 C 节点"));
        assertEquals(ProcessNode.ApproveState.PROCESSING, cView.getApproveState(),
                "C 节点应为审批中");
    }

    private List<FlowRecord> processRecords(FlowRecord anyRecord) {
        return factory.flowRecordRepository.findProcessRecords(anyRecord.getProcessId());
    }

    private List<FlowRecord> recordsOfNode(List<FlowRecord> records, IFlowNode node) {
        return records.stream().filter(record -> node.getId().equals(record.getNodeId())).toList();
    }
}
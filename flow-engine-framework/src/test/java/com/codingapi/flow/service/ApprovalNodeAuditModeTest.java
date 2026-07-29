package com.codingapi.flow.service;

import com.codingapi.flow.action.ActionType;
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
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.ConditionBranchNode;
import com.codingapi.flow.node.nodes.ConditionElseBranchNode;
import com.codingapi.flow.node.nodes.ConditionNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.ParallelBranchNode;
import com.codingapi.flow.node.nodes.ParallelNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowProcessNodeRequest;
import com.codingapi.flow.pojo.response.ProcessNode;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.MultiOperatorAuditStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 审批节点多人审批模式测试
 *
 * <p>覆盖 {@link MultiOperatorAuditStrategy.Type} 的四种审批模式：
 * <ul>
 *     <li>SEQUENCE —— 循序提交：审批人依次办理，后序人员记录先隐藏，前序办理后激活；</li>
 *     <li>MERGE —— 合并审核（并签）：按比例判定节点完成，未达到比例前流程不向下流转；达到比例后剩余未办理记录自动置为已办；</li>
 *     <li>ANY —— 任意审核（或签）：任一人办理后，同节点其他人的待办自动置为已审核，流程向下流转；</li>
 *     <li>RANDOM_ONE —— 随机一人：仅随机一名审批人收到待办，节点只产生一条记录。</li>
 * </ul>
 *
 * <p>同时覆盖嵌套场景：并行节点、条件节点组合岔路下继续存在复杂审批的模式。
 *
 * <p>每个用例均重点验证流程记录数据的准确性：
 * <ul>
 *     <li>记录层面：{@code findProcessRecords} 的记录条数、待办/已办/完成状态、待办分布（隐藏记录不在流程记录中展示）；</li>
 *     <li>视图层面：{@code flowService.processNodes} 的节点顺序、审批状态（PASS/PROCESSING/PENDING）、
 *         审批人展示、审批策略与历史节点数量。</li>
 * </ul>
 */
class ApprovalNodeAuditModeTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    // ==================== 基础工具方法 ====================

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
                .addField("请假事由", "reason", DataType.STRING)
                .build();
    }

    private Map<String, Object> data(int days) {
        return Map.of("name", "lorne", "days", days, "reason", "leave");
    }

    private FormFieldPermissionStrategy perm() {
        return new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                .addPermission("leave", "name", PermissionType.WRITE)
                .addPermission("leave", "days", PermissionType.WRITE)
                .addPermission("leave", "reason", PermissionType.WRITE)
                .build());
    }

    private StartNode startNode() {
        return StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(perm())
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();
    }

    /**
     * 构建审批节点
     *
     * @param name          节点名称
     * @param operatorIds   审批人脚本返回值，如 "[2,3]"
     * @param auditStrategy 多人审批策略，单审批人时传 null
     */
    private ApprovalNode approvalNode(String name, String operatorIds, MultiOperatorAuditStrategy auditStrategy) {
        NodeStrategyBuilder strategyBuilder = NodeStrategyBuilder.builder()
                .addStrategy(perm())
                .addStrategy(new OperatorLoadStrategy(
                        FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return " + operatorIds + "}").getKey()));
        if (auditStrategy != null) {
            strategyBuilder.addStrategy(auditStrategy);
        }
        return ApprovalNode.builder()
                .name(name)
                .strategies(strategyBuilder.build())
                .build();
    }

    private Workflow saveWorkflow(User creator, IFlowNode... nodes) {
        WorkflowBuilder builder = WorkflowBuilder.builder()
                .title("审批流程")
                .code("leave")
                .createdOperator(creator)
                .form(form());
        for (IFlowNode node : nodes) {
            builder.addNode(node);
        }
        Workflow workflow = builder.build();
        factory.workflowService.saveWorkflow(workflow);
        return workflow;
    }

    /**
     * 创建并提交（通过）发起节点的流程
     */
    private void startAndSubmit(Workflow workflow, StartNode startNode, User user, Map<String, Object> data) {
        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(user.getUserId());
        factory.flowService.create(createRequest);

        FlowRecord todo = todoOf(user);
        pass(todo, startNode, user, data);
    }

    /**
     * 以节点第一个动作（通过）办理记录
     */
    private void pass(FlowRecord record, IFlowNode node, User operator, Map<String, Object> data) {
        IFlowAction action = node.actionManager().getActions().get(0);
        FlowActionRequest request = new FlowActionRequest();
        request.setFormData(data);
        request.setRecordId(record.getId());
        request.setAdvice(new FlowAdviceBody(action.id(), "同意", operator.getUserId()));
        factory.flowService.action(request);
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

    private List<FlowRecord> processRecords(FlowRecord anyRecord) {
        return factory.flowRecordRepository.findProcessRecords(anyRecord.getProcessId());
    }

    private List<FlowRecord> recordsOfNode(List<FlowRecord> records, IFlowNode node) {
        return records.stream().filter(r -> r.getNodeId().equals(node.getId())).toList();
    }

    /**
     * 查询流程节点视图（流程记录展示数据）
     */
    private List<ProcessNode> processNodes(FlowRecord record, User viewer, Map<String, Object> data) {
        return factory.flowService.processNodes(new FlowProcessNodeRequest(record.getId(), viewer.getUserId(), data));
    }

    /**
     * 按节点名称从视图中获取唯一节点（用于并行分支等顺序不固定的场景）
     */
    private ProcessNode nodeByName(List<ProcessNode> nodes, String name) {
        List<ProcessNode> found = nodes.stream().filter(n -> name.equals(n.getNodeName())).toList();
        assertEquals(1, found.size(), "节点[" + name + "]应出现且仅出现一次");
        return found.get(0);
    }

    // ==================== 四种审批模式：A(发起) -> B(多人审批) -> C(c1) -> D(结束) ====================

    /**
     * 循序提交（SEQUENCE）：b1、b2 依次审批。
     * <p>b2 的记录初始隐藏、不可办理；b1 办理后激活 b2；b2 办理后流程到 C。
     */
    @Test
    void sequenceAuditWithMultiOperator() {
        User user = new User(1, "user");
        User b1 = new User(2, "b1");
        User b2 = new User(3, "b2");
        User c1 = new User(4, "c1");
        registerUsers(user, b1, b2, c1);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[2,3]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.SEQUENCE, 0));
        ApprovalNode cNode = approvalNode("C审批", "[4]", null);
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(user, startNode, bNode, cNode, endNode);
        Map<String, Object> data = data(1);
        startAndSubmit(workflow, startNode, user, data);

        // b1 收到待办；b2 记录隐藏，待办列表不可见
        FlowRecord b1Todo = todoOf(b1);
        assertNoTodo(b2);

        // 记录数据：隐藏记录不展示在流程记录中，可见 2 条（开始、b1）
        List<FlowRecord> records = processRecords(b1Todo);
        assertEquals(2, records.size());
        List<FlowRecord> bRecords = recordsOfNode(records, bNode);
        assertEquals(1, bRecords.size());
        assertEquals(1, bRecords.stream().filter(FlowRecord::isTodo).count());

        // b1 办理 -> 激活 b2，流程尚未到 C
        pass(b1Todo, bNode, b1, data);
        FlowRecord b2Todo = todoOf(b2);
        assertNoTodo(b1);
        assertNoTodo(c1);

        // b2 记录激活后展示在流程记录中：可见 3 条（开始、b1、b2），B 节点一已办一待办
        records = processRecords(b2Todo);
        assertEquals(3, records.size());
        bRecords = recordsOfNode(records, bNode);
        assertEquals(2, bRecords.size());
        assertEquals(1, bRecords.stream().filter(FlowRecord::isDone).count());
        assertEquals(1, bRecords.stream().filter(FlowRecord::isTodo).count());

        // processNodes 视图：A(通过) -> B(审批中，b1/b2 均展示) -> C(待审批) -> D(待审批)
        List<ProcessNode> nodeList = processNodes(b2Todo, b2, data);
        assertEquals(4, nodeList.size());
        assertEquals(ProcessNode.ApproveState.PASS, nodeList.get(0).getApproveState());
        assertEquals(ProcessNode.ApproveState.PROCESSING, nodeList.get(1).getApproveState());
        assertEquals(ProcessNode.ApproveState.PENDING, nodeList.get(2).getApproveState());
        assertEquals(ProcessNode.ApproveState.PENDING, nodeList.get(3).getApproveState());
        assertEquals(1, nodeList.stream().filter(ProcessNode::isHistory).count());
        ProcessNode bNodeView = nodeList.get(1);
        assertEquals(MultiOperatorAuditStrategy.Type.SEQUENCE, bNodeView.getApproveStrategy());
        assertEquals(2, bNodeView.getOperators().size(), "b2 激活后，B 节点视图应展示两名审批人");

        // b2 办理 -> 流程到 C
        pass(b2Todo, bNode, b2, data);
        FlowRecord c1Todo = todoOf(c1);
        records = processRecords(c1Todo);
        assertEquals(4, records.size());
        assertEquals(2, recordsOfNode(records, bNode).stream().filter(FlowRecord::isDone).count(),
                "B 节点两条记录均应为已办");
        assertEquals(1, records.stream().filter(FlowRecord::isTodo).count(),
                "当前仅应存在 c1 的一条待办");

        // c1 办理 -> 流程结束
        pass(c1Todo, cNode, c1, data);
        records = processRecords(c1Todo);
        assertEquals(4, records.size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).count());
        assertEquals(4, records.stream().filter(FlowRecord::isDone).count());
        assertNoTodo(b1);
        assertNoTodo(b2);
        assertNoTodo(c1);

        // 流程结束后视图：全部节点通过
        nodeList = processNodes(c1Todo, c1, data);
        assertEquals(4, nodeList.size());
        assertEquals(4, nodeList.stream()
                .filter(n -> n.getApproveState() == ProcessNode.ApproveState.PASS).count());
    }

    /**
     * 合并审核（MERGE，比例 1.0）：b1、b2 必须全部审批。
     * <p>b1 办理后节点未完成，流程不向下流转；b2 办理后流程到 C。
     */
    @Test
    void mergeAuditAllPassRequired() {
        User user = new User(1, "user");
        User b1 = new User(2, "b1");
        User b2 = new User(3, "b2");
        User c1 = new User(4, "c1");
        registerUsers(user, b1, b2, c1);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[2,3]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.MERGE, 1.0f));
        ApprovalNode cNode = approvalNode("C审批", "[4]", null);
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(user, startNode, bNode, cNode, endNode);
        Map<String, Object> data = data(1);
        startAndSubmit(workflow, startNode, user, data);

        // b1、b2 同时收到待办
        FlowRecord b1Todo = todoOf(b1);
        FlowRecord b2Todo = todoOf(b2);

        List<FlowRecord> records = processRecords(b1Todo);
        assertEquals(3, records.size());
        assertEquals(2, recordsOfNode(records, bNode).stream().filter(FlowRecord::isTodo).count());

        // b1 办理 -> 比例未达到，流程不向下流转
        pass(b1Todo, bNode, b1, data);
        assertNoTodo(c1);
        records = processRecords(b2Todo);
        assertEquals(3, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isDone).count(), "开始记录与 b1 记录应为已办");
        assertEquals(1, records.stream().filter(FlowRecord::isTodo).count(), "b2 仍为待办");

        // processNodes 视图：B 节点审批中（并签策略，展示两名审批人），后续节点待审批
        List<ProcessNode> nodeList = processNodes(b2Todo, b2, data);
        assertEquals(4, nodeList.size());
        assertEquals(ProcessNode.ApproveState.PASS, nodeList.get(0).getApproveState());
        assertEquals(ProcessNode.ApproveState.PROCESSING, nodeList.get(1).getApproveState());
        assertEquals(ProcessNode.ApproveState.PENDING, nodeList.get(2).getApproveState());
        assertEquals(ProcessNode.ApproveState.PENDING, nodeList.get(3).getApproveState());
        ProcessNode bNodeView = nodeList.get(1);
        assertEquals(MultiOperatorAuditStrategy.Type.MERGE, bNodeView.getApproveStrategy());
        assertEquals(2, bNodeView.getOperators().size());

        // b2 办理 -> 比例达到，流程到 C
        pass(b2Todo, bNode, b2, data);
        FlowRecord c1Todo = todoOf(c1);
        records = processRecords(c1Todo);
        assertEquals(4, records.size());
        assertEquals(2, recordsOfNode(records, bNode).stream().filter(FlowRecord::isDone).count());
        assertEquals(1, records.stream().filter(FlowRecord::isTodo).count());

        // c1 办理 -> 流程结束
        pass(c1Todo, cNode, c1, data);
        records = processRecords(c1Todo);
        assertEquals(4, records.size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).count());
        assertNoTodo(b1);
        assertNoTodo(b2);
        assertNoTodo(c1);

        // 流程结束后视图：全部节点通过
        nodeList = processNodes(c1Todo, c1, data);
        assertEquals(4, nodeList.size());
        assertEquals(4, nodeList.stream().filter(ProcessNode::isHistory).count());
    }

    /**
     * 连续两个会签（MERGE）节点：A(发起) -> B(b1、b2 会签) -> C(c1、c2 会签) -> D(结束)。
     *
     * <p>问题复现：B 节点仅 b1 一人提交（会签比例未达成，流程不应向下流转）后，
     * 查看流程记录时，出现了两个 C 节点记录、每条记录各有一个审批人（c1、c2）的脏数据。
     *
     * <p>期望：B 节点未完成前，流程记录中不应存在任何 C 节点记录；
     * processNodes 视图中 C 节点应仅出现一次且处于待审批（PENDING）状态。
     */
    @Test
    void mergeAuditNextMergeNodeRecordsShouldNotAppearPrematurely() {
        User user = new User(1, "user");
        User b1 = new User(2, "b1");
        User b2 = new User(3, "b2");
        User c1 = new User(4, "c1");
        User c2 = new User(8, "c2");
        registerUsers(user, b1, b2, c1, c2);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[2,3]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.MERGE, 1.0f));
        ApprovalNode cNode = approvalNode("C审批", "[4,8]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.MERGE, 1.0f));
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(user, startNode, bNode, cNode, endNode);
        Map<String, Object> data = data(1);
        startAndSubmit(workflow, startNode, user, data);

        // B 节点：b1、b2 同时收到待办；C 节点审批人不应产生任何待办
        FlowRecord b1Todo = todoOf(b1);
        FlowRecord b2Todo = todoOf(b2);
        assertNoTodo(c1);
        assertNoTodo(c2);

        // 初始流程记录：开始、b1、b2 共 3 条，C 节点尚无任何记录
        List<FlowRecord> records = processRecords(b1Todo);
        assertEquals(3, records.size());
        assertEquals(0, recordsOfNode(records, cNode).size(), "B 节点未完成前不应产生 C 节点记录");

        // b1 办理 -> 会签比例 1/2 未达成，流程不向下流转
        pass(b1Todo, bNode, b1, data);
        assertNoTodo(c1);
        assertNoTodo(c2);

        // ============ 核心验证：流程记录中不应出现 C 节点脏数据 ============
        records = processRecords(b2Todo);
        // 应为：开始、b1(已办)、b2(待办) 共 3 条，绝不允许出现 C 节点记录
        assertEquals(3, records.size(), "B 节点未完成时，流程记录应仅有 开始、b1、b2 三条");
        assertEquals(0, recordsOfNode(records, cNode).size(),
                "B 节点会签未完成前，流程记录中不应出现任何 C 节点记录");
        assertEquals(2, recordsOfNode(records, bNode).size());
        assertEquals(1, records.stream().filter(FlowRecord::isTodo).count(), "仅 b2 一条待办");

        // ============ 视图验证：C 节点仅出现一次且为待审批 ============
        List<ProcessNode> nodeList = processNodes(b2Todo, b2, data);
        assertEquals(4, nodeList.size());
        assertEquals(ProcessNode.ApproveState.PASS, nodeList.get(0).getApproveState());
        assertEquals(ProcessNode.ApproveState.PROCESSING, nodeList.get(1).getApproveState());
        assertEquals(ProcessNode.ApproveState.PENDING, nodeList.get(2).getApproveState());
        assertEquals(ProcessNode.ApproveState.PENDING, nodeList.get(3).getApproveState());
        // C 节点在视图中应仅出现一次，不应出现两个 C 节点记录
        assertEquals(1, nodeList.stream()
                        .filter(n -> cNode.getName().equals(n.getNodeName())).count(),
                "C 节点在流程节点视图中应仅出现一次，不应出现两条 C 节点记录");
    }

    /**
     * 合并审核（MERGE，比例 0.5）：b1、b2、b3 三人，两人办理即达到比例。
     * <p>达到比例后节点完成，剩余未办理的 b3 记录自动置为已办（待办清空），流程到 C。
     */
    @Test
    void mergeAuditByPercentAutoDone() {
        User user = new User(1, "user");
        User b1 = new User(2, "b1");
        User b2 = new User(3, "b2");
        User b3 = new User(5, "b3");
        User c1 = new User(4, "c1");
        registerUsers(user, b1, b2, b3, c1);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[2,3,5]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.MERGE, 0.5f));
        ApprovalNode cNode = approvalNode("C审批", "[4]", null);
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(user, startNode, bNode, cNode, endNode);
        Map<String, Object> data = data(1);
        startAndSubmit(workflow, startNode, user, data);

        // b1、b2、b3 同时收到待办
        FlowRecord b1Todo = todoOf(b1);
        todoOf(b2);
        todoOf(b3);

        // b1 办理 -> 1/3 未达比例，流程不向下流转
        pass(b1Todo, bNode, b1, data);
        assertNoTodo(c1);
        List<FlowRecord> records = processRecords(b1Todo);
        assertEquals(4, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isTodo).count(), "b2、b3 仍为待办");

        // b2 办理 -> 2/3 达到比例，b3 自动置为已办，流程到 C
        FlowRecord b2Todo = todoOf(b2);
        pass(b2Todo, bNode, b2, data);
        assertNoTodo(b3);
        FlowRecord c1Todo = todoOf(c1);

        records = processRecords(c1Todo);
        assertEquals(5, records.size());
        List<FlowRecord> bRecords = recordsOfNode(records, bNode);
        assertEquals(3, bRecords.size());
        assertEquals(3, bRecords.stream().filter(FlowRecord::isDone).count(),
                "B 节点三条记录均应为已办（含 b3 自动办结）");
        assertEquals(1, records.stream().filter(FlowRecord::isTodo).count(), "当前仅应存在 c1 的一条待办");

        // processNodes 视图：B 节点已通过（并签比例达成），展示 3 名审批人记录
        List<ProcessNode> nodeList = processNodes(c1Todo, c1, data);
        assertEquals(4, nodeList.size());
        assertEquals(ProcessNode.ApproveState.PASS, nodeList.get(0).getApproveState());
        assertEquals(ProcessNode.ApproveState.PASS, nodeList.get(1).getApproveState());
        assertEquals(ProcessNode.ApproveState.PROCESSING, nodeList.get(2).getApproveState());
        assertEquals(ProcessNode.ApproveState.PENDING, nodeList.get(3).getApproveState());
        assertEquals(3, nodeList.get(1).getOperators().size(),
                "B 节点视图应展示 b1、b2、b3 三条审批记录");

        // c1 办理 -> 流程结束
        pass(c1Todo, cNode, c1, data);
        records = processRecords(c1Todo);
        assertEquals(5, records.size());
        assertEquals(5, records.stream().filter(FlowRecord::isFinish).count());
        assertNoTodo(b1);
        assertNoTodo(b2);
        assertNoTodo(b3);
        assertNoTodo(c1);

        // 流程结束后视图：全部节点通过
        nodeList = processNodes(c1Todo, c1, data);
        assertEquals(4, nodeList.size());
        assertEquals(4, nodeList.stream().filter(ProcessNode::isHistory).count());
    }

    /**
     * 任意审核（ANY，或签）场景回归测试（由 FlowSampleServiceTest 迁移）。
     *
     * <p>流程设计：A(发起节点) -> B(b1、b2 或签审批) -> C(c1 普通审批) -> D(结束节点)。
     * <p>操作步骤：发起人提交 -> B 节点（b1、b2 同时收到待办）-> b1 任意一人审批通过 -> c1 审批通过。
     * <p>期望验证三点：
     * <p>1. 流程正常：b1 审批后，b2 的待办自动变为已审核（待办清空），流程流转到 C 节点（c1 收到待办）；c1 审批后流程结束。
     * <p>2. 流程记录数据正常：B 节点两条记录（b1、b2）均为已办状态，不存在遗留的待办记录；流程结束后所有记录均为完成状态。
     * <p>3. processNodes 流程节点视图数据正常：节点顺序、审批状态、审批人与审批策略展示准确。
     */
    @Test
    void anyAuditWithMultiOperator() {
        User user = new User(1, "user");
        User b1 = new User(2, "b1");
        User b2 = new User(3, "b2");
        User c1 = new User(4, "c1");
        registerUsers(user, b1, b2, c1);

        StartNode startNode = startNode();
        // B 审批节点：b1、b2 或签（任意审核）
        ApprovalNode bNode = approvalNode("B审批", "[2,3]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0));
        // C 审批节点：c1 普通审批
        ApprovalNode cNode = approvalNode("C审批", "[4]", null);
        // D 结束节点
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(user, startNode, bNode, cNode, endNode);
        Map<String, Object> data = data(1);

        // 1. 发起人提交
        startAndSubmit(workflow, startNode, user, data);

        // 2. 流程流转到 B 节点：b1、b2 应同时收到待办
        FlowRecord b1Todo = todoOf(b1);
        todoOf(b2);

        // 3. b1（任意一人）审批通过
        pass(b1Todo, bNode, b1, data);

        // ============ 验证点 1：流程是否正常 ============
        // b1 或签通过后，b2 的待办应自动变为已审核（待办清空）
        assertNoTodo(b2);
        // 流程应流转到 C 节点：c1 收到待办
        FlowRecord c1Todo = todoOf(c1);

        // ============ 验证点 2：流程记录数据是否正常 ============
        List<FlowRecord> records = processRecords(c1Todo);
        // A、B(b1)、B(b2)、C 共 4 条记录
        assertEquals(4, records.size());

        // B 节点的两条记录都应为已办（b1 审批通过、b2 或签自动已审核）
        List<FlowRecord> bNodeRecords = recordsOfNode(records, bNode);
        assertEquals(2, bNodeRecords.size());
        assertEquals(2, bNodeRecords.stream().filter(FlowRecord::isDone).count(),
                "或签模式下，一人审批后另一人的记录也应变为已办（已审核）");

        // 当前仅应存在 c1 的一条待办记录，不允许有遗留待办
        assertEquals(1, records.stream().filter(FlowRecord::isTodo).count(),
                "当前仅应存在 c1 的一条待办记录");

        // ============ 验证点 3：processNodes 流程节点视图数据 ============
        // 视图顺序：A(通过) -> B(通过) -> C(审批中) -> D(待审批)
        List<ProcessNode> nodeList = processNodes(c1Todo, c1, data);
        assertEquals(4, nodeList.size());
        assertEquals(startNode.getName(), nodeList.get(0).getNodeName());
        assertEquals(bNode.getName(), nodeList.get(1).getNodeName());
        assertEquals(cNode.getName(), nodeList.get(2).getNodeName());
        assertEquals(endNode.getName(), nodeList.get(3).getNodeName());
        assertEquals(ProcessNode.ApproveState.PASS, nodeList.get(0).getApproveState());
        assertEquals(ProcessNode.ApproveState.PASS, nodeList.get(1).getApproveState());
        assertEquals(ProcessNode.ApproveState.PROCESSING, nodeList.get(2).getApproveState());
        assertEquals(ProcessNode.ApproveState.PENDING, nodeList.get(3).getApproveState());
        assertEquals(2, nodeList.stream().filter(ProcessNode::isHistory).count());

        // B 节点视图：或签策略，b1、b2 两名审批人均展示（b2 为自动已审核，无审批动作）
        ProcessNode bNodeView = nodeList.get(1);
        assertEquals(MultiOperatorAuditStrategy.Type.ANY, bNodeView.getApproveStrategy());
        assertEquals(2, bNodeView.getOperators().size());
        assertEquals(1, bNodeView.getOperators().stream()
                        .filter(o -> ActionType.PASS.name().equals(o.getActionType())).count(),
                "B 节点视图中应仅有 b1 一条通过动作记录");

        // C 节点视图：展示审批人 c1
        assertEquals(1, nodeList.get(2).getOperators().size());

        // 4. c1 审批通过，流程结束
        pass(c1Todo, cNode, c1, data);

        // 流程结束后：所有记录完成，任何人无待办
        records = processRecords(c1Todo);
        assertEquals(4, records.size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).count());
        assertEquals(0, records.stream().filter(FlowRecord::isTodo).count());
        assertNoTodo(b1);
        assertNoTodo(b2);
        assertNoTodo(c1);

        // 流程结束后视图：全部节点通过（历史）
        nodeList = processNodes(c1Todo, c1, data);
        assertEquals(4, nodeList.size());
        assertEquals(4, nodeList.stream().filter(ProcessNode::isHistory).count());
        assertEquals(4, nodeList.stream()
                .filter(n -> n.getApproveState() == ProcessNode.ApproveState.PASS).count());
    }

    /**
     * 任意审核（ANY，或签）：b2 先办理（验证与办理顺序无关）。
     * <p>b2 办理后，b1 的待办自动变为已审核（待办清空），流程到 C。
     */
    @Test
    void anyAuditBySecondOperator() {
        User user = new User(1, "user");
        User b1 = new User(2, "b1");
        User b2 = new User(3, "b2");
        User c1 = new User(4, "c1");
        registerUsers(user, b1, b2, c1);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[2,3]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0));
        ApprovalNode cNode = approvalNode("C审批", "[4]", null);
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(user, startNode, bNode, cNode, endNode);
        Map<String, Object> data = data(1);
        startAndSubmit(workflow, startNode, user, data);

        todoOf(b1);
        FlowRecord b2Todo = todoOf(b2);

        // b2 办理 -> b1 自动已审核，流程到 C
        pass(b2Todo, bNode, b2, data);
        assertNoTodo(b1);
        FlowRecord c1Todo = todoOf(c1);

        List<FlowRecord> records = processRecords(c1Todo);
        assertEquals(4, records.size());
        assertEquals(2, recordsOfNode(records, bNode).stream().filter(FlowRecord::isDone).count());
        assertEquals(1, records.stream().filter(FlowRecord::isTodo).count());

        // processNodes 视图：B 节点已通过，两名审批人均展示
        List<ProcessNode> nodeList = processNodes(c1Todo, c1, data);
        assertEquals(4, nodeList.size());
        assertEquals(ProcessNode.ApproveState.PASS, nodeList.get(1).getApproveState());
        assertEquals(ProcessNode.ApproveState.PROCESSING, nodeList.get(2).getApproveState());
        assertEquals(2, nodeList.get(1).getOperators().size());

        // c1 办理 -> 流程结束
        pass(c1Todo, cNode, c1, data);
        records = processRecords(c1Todo);
        assertEquals(4, records.size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).count());
        assertNoTodo(b1);
        assertNoTodo(b2);
        assertNoTodo(c1);

        // 流程结束后视图：全部节点通过
        nodeList = processNodes(c1Todo, c1, data);
        assertEquals(4, nodeList.size());
        assertEquals(4, nodeList.stream().filter(ProcessNode::isHistory).count());
    }

    /**
     * 随机一人（RANDOM_ONE）：b1、b2 中仅随机一人收到待办。
     * <p>B 节点只产生一条记录；该审批人办理后流程到 C。
     */
    @Test
    void randomOneAudit() {
        User user = new User(1, "user");
        User b1 = new User(2, "b1");
        User b2 = new User(3, "b2");
        User c1 = new User(4, "c1");
        registerUsers(user, b1, b2, c1);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[2,3]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.RANDOM_ONE, 0));
        ApprovalNode cNode = approvalNode("C审批", "[4]", null);
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(user, startNode, bNode, cNode, endNode);
        Map<String, Object> data = data(1);
        startAndSubmit(workflow, startNode, user, data);

        // b1、b2 中有且仅有一人收到待办
        List<FlowRecord> b1Todos = factory.flowRecordRepository.findTodoByOperator(b1.getUserId());
        List<FlowRecord> b2Todos = factory.flowRecordRepository.findTodoByOperator(b2.getUserId());
        assertEquals(1, b1Todos.size() + b2Todos.size(), "随机一人模式应只有一名审批人收到待办");

        boolean b1Chosen = b1Todos.size() == 1;
        User chosen = b1Chosen ? b1 : b2;
        FlowRecord chosenTodo = b1Chosen ? b1Todos.get(0) : b2Todos.get(0);

        // B 节点只产生一条记录：可见记录为开始 + B 节点记录共 2 条
        List<FlowRecord> records = processRecords(chosenTodo);
        assertEquals(2, records.size());
        assertEquals(1, recordsOfNode(records, bNode).size(), "随机一人模式 B 节点应只有一条记录");

        // 选中的审批人办理 -> 流程到 C
        pass(chosenTodo, bNode, chosen, data);
        FlowRecord c1Todo = todoOf(c1);

        records = processRecords(c1Todo);
        assertEquals(3, records.size());
        assertEquals(1, records.stream().filter(FlowRecord::isTodo).count());

        // processNodes 视图：B 节点视图仅展示随机选中的一名审批人
        List<ProcessNode> nodeList = processNodes(c1Todo, c1, data);
        assertEquals(4, nodeList.size());
        assertEquals(ProcessNode.ApproveState.PASS, nodeList.get(1).getApproveState());
        assertEquals(ProcessNode.ApproveState.PROCESSING, nodeList.get(2).getApproveState());
        ProcessNode bNodeView = nodeList.get(1);
        assertEquals(MultiOperatorAuditStrategy.Type.RANDOM_ONE, bNodeView.getApproveStrategy());
        assertEquals(1, bNodeView.getOperators().size(), "随机一人模式视图应仅有一名审批人");

        // c1 办理 -> 流程结束
        pass(c1Todo, cNode, c1, data);
        records = processRecords(c1Todo);
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).count());
        assertNoTodo(b1);
        assertNoTodo(b2);
        assertNoTodo(c1);

        // 流程结束后视图：全部节点通过
        nodeList = processNodes(c1Todo, c1, data);
        assertEquals(4, nodeList.size());
        assertEquals(4, nodeList.stream().filter(ProcessNode::isHistory).count());
    }

    /**
     * 会签节点存在多个待办时，processNodes 视图不应重复展示下游节点（下游 C 为会签）。
     *
     * <p>流程设计：A(发起) -> B(b1、b2 会签) -> C(c1、c2 会签) -> D(结束)。
     * <p>问题复现：B 节点为多人审批（会签/或签），存在 b1、b2 两条待办记录时，
     * 查看流程节点视图（{@code processNodes}）会出现两个 C 节点记录（每个下游节点被重复展示），
     * 而非期望的一个 C 节点。
     * <p>期望：无论当前节点有多少条待办，其下游节点在视图中应仅展示一次。
     */
    @Test
    void processNodesShouldNotDuplicateNextNodeWhenNextIsMerge() {
        assertNextNodeNotDuplicated(
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.MERGE, 1.0f));
    }

    /**
     * 会签节点存在多个待办时，processNodes 视图不应重复展示下游节点（下游 C 为或签）。
     * <p>验证下游节点审批模式为任意审核（ANY）时同样不会重复展示。
     */
    @Test
    void processNodesShouldNotDuplicateNextNodeWhenNextIsAny() {
        assertNextNodeNotDuplicated(
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0));
    }

    /**
     * 断言：B 会签节点存在 b1、b2 两条待办时，从 b1 待办查看流程节点视图，
     * 下游 C 节点（按 {@code cAuditStrategy} 配置）仅展示一次。
     */
    private void assertNextNodeNotDuplicated(MultiOperatorAuditStrategy cAuditStrategy) {
        User user = new User(1, "user");
        User b1 = new User(2, "b1");
        User b2 = new User(3, "b2");
        User c1 = new User(4, "c1");
        User c2 = new User(8, "c2");
        registerUsers(user, b1, b2, c1, c2);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[2,3]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.MERGE, 1.0f));
        ApprovalNode cNode = approvalNode("C审批", "[4,8]", cAuditStrategy);
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(user, startNode, bNode, cNode, endNode);
        Map<String, Object> data = data(1);
        startAndSubmit(workflow, startNode, user, data);

        // B 节点：b1、b2 同时收到待办（同一节点存在多条待办）
        FlowRecord b1Todo = todoOf(b1);
        todoOf(b2);

        // 从 b1 待办查看流程节点视图：下游 C 节点应仅展示一次
        List<ProcessNode> nodeList = processNodes(b1Todo, b1, data);
        assertEquals(4, nodeList.size(), "视图应仅含 开始、B、C、结束 四个节点，下游节点不应重复");
        assertEquals(1, nodeList.stream()
                        .filter(n -> cNode.getName().equals(n.getNodeName())).count(),
                "C 节点在流程节点视图中应仅出现一次，不应因 B 节点有多条待办而重复展示");
        assertEquals(1, nodeList.stream()
                        .filter(n -> bNode.getName().equals(n.getNodeName())).count(),
                "B 节点在视图中应仅出现一次");
        // 下游 C 节点视图应完整展示 c1、c2 两名审批人
        ProcessNode cNodeView = nodeList.stream()
                .filter(n -> cNode.getName().equals(n.getNodeName())).findFirst().orElseThrow();
        assertEquals(2, cNodeView.getOperators().size(), "C 节点视图应展示 c1、c2 两名审批人");
    }

    // ==================== 嵌套场景：并行 / 条件岔路下的复杂审批 ====================

    /**
     * 并行节点双分支复杂审批：分支1 为或签（b1、b2），分支2 为循序审批（d1、d2）。
     * <p>重点验证：任一分支完成不会结束流程（并行汇聚等待）；或签自动办结、循序激活均正常；
     * 全部分支完成后流程记录全部为已完成状态；processNodes 视图数据准确。
     */
    @Test
    void parallelBranchesWithAnyAndSequenceAudit() {
        User user = new User(1, "user");
        User b1 = new User(2, "b1");
        User b2 = new User(3, "b2");
        User d1 = new User(6, "d1");
        User d2 = new User(7, "d2");
        registerUsers(user, b1, b2, d1, d2);

        StartNode startNode = startNode();
        ApprovalNode anyNode = approvalNode("B或签审批", "[2,3]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0));
        ApprovalNode sequenceNode = approvalNode("E循序审批", "[6,7]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.SEQUENCE, 0));

        ParallelBranchNode branch1 = ParallelBranchNode.builder()
                .name("并行分支1").order(1).blocks(anyNode).build();
        ParallelBranchNode branch2 = ParallelBranchNode.builder()
                .name("并行分支2").order(2).blocks(sequenceNode).build();
        ParallelNode parallelNode = ParallelNode.builder()
                .name("并行控制").blocks(branch1, branch2).build();
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(user, startNode, parallelNode, endNode);
        Map<String, Object> data = data(1);
        startAndSubmit(workflow, startNode, user, data);

        // 两个分支同时启动：b1、b2（或签）与 d1（循序首位）收到待办，d2 隐藏
        FlowRecord b1Todo = todoOf(b1);
        todoOf(b2);
        FlowRecord d1Todo = todoOf(d1);
        assertNoTodo(d2);

        List<FlowRecord> records = processRecords(b1Todo);
        assertEquals(4, records.size(), "应为：开始、b1、b2、d1 共 4 条记录");
        assertEquals(3, records.stream().filter(FlowRecord::isTodo).count());
        assertEquals(0, records.stream().filter(FlowRecord::isFinish).count());

        // 分支1：b1 或签办理 -> b2 自动已审核；分支2 未完成，流程不结束
        pass(b1Todo, anyNode, b1, data);
        assertNoTodo(b2);
        todoOf(d1);
        assertNoTodo(d2);

        records = processRecords(d1Todo);
        assertEquals(4, records.size());
        assertEquals(2, recordsOfNode(records, anyNode).stream().filter(FlowRecord::isDone).count(),
                "或签分支两条记录均应为已办");
        assertEquals(1, records.stream().filter(FlowRecord::isTodo).count(), "仅 d1 一条待办");
        assertEquals(0, records.stream().filter(FlowRecord::isFinish).count(),
                "分支2 未完成，流程不应结束");

        // processNodes 视图：并行分支顺序不固定，按节点名称核实
        List<ProcessNode> nodeList = processNodes(d1Todo, d1, data);
        assertEquals(4, nodeList.size());
        assertEquals(ProcessNode.ApproveState.PASS,
                nodeByName(nodeList, startNode.getName()).getApproveState());
        ProcessNode anyNodeView = nodeByName(nodeList, anyNode.getName());
        assertEquals(ProcessNode.ApproveState.PASS, anyNodeView.getApproveState());
        assertEquals(2, anyNodeView.getOperators().size(), "或签分支视图应展示 b1、b2 两条记录");
        ProcessNode sequenceNodeView = nodeByName(nodeList, sequenceNode.getName());
        assertEquals(ProcessNode.ApproveState.PROCESSING, sequenceNodeView.getApproveState());
        assertEquals(1, sequenceNodeView.getOperators().size(), "d2 未激活（隐藏），视图不应展示");
        assertEquals(ProcessNode.ApproveState.PENDING,
                nodeByName(nodeList, endNode.getName()).getApproveState());
        assertEquals(2, nodeList.stream().filter(ProcessNode::isHistory).count());

        // 分支2：d1 办理 -> 激活 d2
        pass(d1Todo, sequenceNode, d1, data);
        FlowRecord d2Todo = todoOf(d2);

        // 分支2：d2 办理 -> 并行汇聚完成，流程结束
        pass(d2Todo, sequenceNode, d2, data);
        // d2 记录已被循序激活展示，可见记录共 5 条（开始、b1、b2、d1、d2）
        records = processRecords(d2Todo);
        assertEquals(5, records.size());
        assertEquals(5, records.stream().filter(FlowRecord::isFinish).count(), "流程结束后全部记录应为完成状态");
        assertEquals(5, records.stream().filter(FlowRecord::isDone).count());
        assertEquals(0, records.stream().filter(FlowRecord::isTodo).count());
        assertNoTodo(b1);
        assertNoTodo(b2);
        assertNoTodo(d1);
        assertNoTodo(d2);

        // 流程结束后视图：全部节点通过；d2 激活后循序分支视图展示两名审批人
        nodeList = processNodes(d2Todo, d2, data);
        assertEquals(4, nodeList.size());
        assertEquals(4, nodeList.stream().filter(ProcessNode::isHistory).count());
        assertEquals(2, nodeByName(nodeList, sequenceNode.getName()).getOperators().size());
    }

    /**
     * 条件分支（if 路径）下的并签审批：days >= 3 进入并签分支（b1、b2，比例 1.0）。
     * <p>验证条件路由准确，且并签全部办理后流程正常结束、记录数据准确。
     */
    @Test
    void conditionBranchWithMergeAudit() {
        User user = new User(1, "user");
        User b1 = new User(2, "b1");
        User b2 = new User(3, "b2");
        User c1 = new User(4, "c1");
        User c2 = new User(8, "c2");
        registerUsers(user, b1, b2, c1, c2);

        StartNode startNode = startNode();
        ApprovalNode mergeNode = approvalNode("B并签审批", "[2,3]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.MERGE, 1.0f));
        ApprovalNode anyNode = approvalNode("C或签审批", "[4,8]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0));

        ConditionBranchNode ifBranch = ConditionBranchNode.builder()
                .name("条件分支")
                .conditionScript(FlowGroovyScriptFactory.createConditionScript(
                        "def run(request){return request.getFormData('days') >= 3}").getKey())
                .order(1)
                .blocks(mergeNode)
                .build();
        ConditionElseBranchNode elseBranch = ConditionElseBranchNode.builder()
                .name("else条件分支").order(2).blocks(anyNode).build();
        ConditionNode conditionNode = ConditionNode.builder()
                .name("条件控制").blocks(ifBranch, elseBranch).build();
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(user, startNode, conditionNode, endNode);
        // days = 5，命中 if 分支（并签）
        Map<String, Object> data = data(5);
        startAndSubmit(workflow, startNode, user, data);

        // 仅并签分支收到待办，else 分支的 c1、c2 不应产生记录
        FlowRecord b1Todo = todoOf(b1);
        FlowRecord b2Todo = todoOf(b2);
        assertNoTodo(c1);
        assertNoTodo(c2);

        List<FlowRecord> records = processRecords(b1Todo);
        assertEquals(3, records.size(), "应为：开始、b1、b2 共 3 条记录（else 分支不应产生记录）");
        assertEquals(0, recordsOfNode(records, anyNode).size());

        // b1 办理 -> 比例未达到，流程未结束
        pass(b1Todo, mergeNode, b1, data);
        records = processRecords(b2Todo);
        assertEquals(3, records.size());
        assertEquals(1, records.stream().filter(FlowRecord::isTodo).count());
        assertEquals(0, records.stream().filter(FlowRecord::isFinish).count(), "并签未完成，流程不应结束");

        // processNodes 视图：A(通过) -> B 并签(审批中) -> D(待审批)
        List<ProcessNode> nodeList = processNodes(b2Todo, b2, data);
        assertEquals(3, nodeList.size());
        assertEquals(ProcessNode.ApproveState.PASS, nodeList.get(0).getApproveState());
        assertEquals(ProcessNode.ApproveState.PROCESSING, nodeList.get(1).getApproveState());
        assertEquals(ProcessNode.ApproveState.PENDING, nodeList.get(2).getApproveState());
        ProcessNode bNodeView = nodeList.get(1);
        assertEquals(mergeNode.getName(), bNodeView.getNodeName());
        assertEquals(MultiOperatorAuditStrategy.Type.MERGE, bNodeView.getApproveStrategy());
        assertEquals(2, bNodeView.getOperators().size());

        // b2 办理 -> 并签完成，直达结束节点，流程结束
        pass(b2Todo, mergeNode, b2, data);
        records = processRecords(b2Todo);
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).count());
        assertEquals(3, records.stream().filter(FlowRecord::isDone).count());
        assertNoTodo(b1);
        assertNoTodo(b2);

        // 流程结束后视图：全部节点通过
        nodeList = processNodes(b2Todo, b2, data);
        assertEquals(3, nodeList.size());
        assertEquals(3, nodeList.stream().filter(ProcessNode::isHistory).count());
    }

    /**
     * 条件分支（else 路径）下的或签审批：days < 3 进入 else 或签分支（c1、c2）。
     * <p>验证 else 路由准确，且嵌套在条件分支下的或签自动办结逻辑正常。
     */
    @Test
    void conditionElseBranchWithAnyAudit() {
        User user = new User(1, "user");
        User b1 = new User(2, "b1");
        User b2 = new User(3, "b2");
        User c1 = new User(4, "c1");
        User c2 = new User(8, "c2");
        registerUsers(user, b1, b2, c1, c2);

        StartNode startNode = startNode();
        ApprovalNode mergeNode = approvalNode("B并签审批", "[2,3]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.MERGE, 1.0f));
        ApprovalNode anyNode = approvalNode("C或签审批", "[4,8]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0));

        ConditionBranchNode ifBranch = ConditionBranchNode.builder()
                .name("条件分支")
                .conditionScript(FlowGroovyScriptFactory.createConditionScript(
                        "def run(request){return request.getFormData('days') >= 3}").getKey())
                .order(1)
                .blocks(mergeNode)
                .build();
        ConditionElseBranchNode elseBranch = ConditionElseBranchNode.builder()
                .name("else条件分支").order(2).blocks(anyNode).build();
        ConditionNode conditionNode = ConditionNode.builder()
                .name("条件控制").blocks(ifBranch, elseBranch).build();
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(user, startNode, conditionNode, endNode);
        // days = 1，命中 else 分支（或签）
        Map<String, Object> data = data(1);
        startAndSubmit(workflow, startNode, user, data);

        // 仅 else 或签分支收到待办，if 分支的 b1、b2 不应产生记录
        todoOf(c1);
        FlowRecord c2Todo = todoOf(c2);
        assertNoTodo(b1);
        assertNoTodo(b2);

        List<FlowRecord> records = processRecords(c2Todo);
        assertEquals(3, records.size(), "应为：开始、c1、c2 共 3 条记录（if 分支不应产生记录）");
        assertEquals(0, recordsOfNode(records, mergeNode).size());

        // processNodes 视图：A(通过) -> C 或签(审批中) -> D(待审批)
        List<ProcessNode> nodeList = processNodes(c2Todo, c2, data);
        assertEquals(3, nodeList.size());
        assertEquals(ProcessNode.ApproveState.PASS, nodeList.get(0).getApproveState());
        assertEquals(ProcessNode.ApproveState.PROCESSING, nodeList.get(1).getApproveState());
        assertEquals(ProcessNode.ApproveState.PENDING, nodeList.get(2).getApproveState());
        ProcessNode anyNodeView = nodeList.get(1);
        assertEquals(anyNode.getName(), anyNodeView.getNodeName());
        assertEquals(MultiOperatorAuditStrategy.Type.ANY, anyNodeView.getApproveStrategy());
        assertEquals(2, anyNodeView.getOperators().size());

        // c2 办理（任意一人）-> c1 自动已审核，直达结束节点，流程结束
        pass(c2Todo, anyNode, c2, data);
        assertNoTodo(c1);
        records = processRecords(c2Todo);
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).count());
        assertEquals(2, recordsOfNode(records, anyNode).stream().filter(FlowRecord::isDone).count(),
                "或签分支两条记录均应为已办");
        assertNoTodo(c1);
        assertNoTodo(c2);

        // 流程结束后视图：全部节点通过，或签分支视图展示两名审批人
        nodeList = processNodes(c2Todo, c2, data);
        assertEquals(3, nodeList.size());
        assertEquals(3, nodeList.stream().filter(ProcessNode::isHistory).count());
        assertEquals(2, nodeByName(nodeList, anyNode.getName()).getOperators().size());
    }

    /**
     * 深层嵌套：并行分支1 内嵌条件节点（if 或签 b1/b2，else 普通审批 c1），并行分支2 为普通审批 d1。
     * <p>验证并行 + 条件 + 多人审批组合下，流程记录数据与 processNodes 视图数据的准确性：
     * 记录条数、各阶段待办分布、或签自动办结、并行汇聚前流程不结束、汇聚后全部完成。
     */
    @Test
    void parallelNestedConditionWithAnyAudit() {
        User user = new User(1, "user");
        User b1 = new User(2, "b1");
        User b2 = new User(3, "b2");
        User c1 = new User(4, "c1");
        User d1 = new User(6, "d1");
        registerUsers(user, b1, b2, c1, d1);

        StartNode startNode = startNode();
        ApprovalNode anyNode = approvalNode("B或签审批", "[2,3]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0));
        ApprovalNode singleCNode = approvalNode("C普通审批", "[4]", null);

        ConditionBranchNode ifBranch = ConditionBranchNode.builder()
                .name("条件分支")
                .conditionScript(FlowGroovyScriptFactory.createConditionScript(
                        "def run(request){return request.getFormData('days') >= 3}").getKey())
                .order(1)
                .blocks(anyNode)
                .build();
        ConditionElseBranchNode elseBranch = ConditionElseBranchNode.builder()
                .name("else条件分支").order(2).blocks(singleCNode).build();
        ConditionNode conditionNode = ConditionNode.builder()
                .name("条件控制").blocks(ifBranch, elseBranch).build();

        ApprovalNode singleDNode = approvalNode("D普通审批", "[6]", null);

        ParallelBranchNode branch1 = ParallelBranchNode.builder()
                .name("并行分支1").order(1).blocks(conditionNode).build();
        ParallelBranchNode branch2 = ParallelBranchNode.builder()
                .name("并行分支2").order(2).blocks(singleDNode).build();
        ParallelNode parallelNode = ParallelNode.builder()
                .name("并行控制").blocks(branch1, branch2).build();
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(user, startNode, parallelNode, endNode);
        // days = 5，分支1 命中 if 路径（或签 b1/b2）
        Map<String, Object> data = data(5);
        startAndSubmit(workflow, startNode, user, data);

        // 分支1（条件 if 或签）与分支2（普通审批）同时启动；else 分支 c1 不应产生记录
        FlowRecord b1Todo = todoOf(b1);
        todoOf(b2);
        FlowRecord d1Todo = todoOf(d1);
        assertNoTodo(c1);

        List<FlowRecord> records = processRecords(b1Todo);
        assertEquals(4, records.size(), "应为：开始、b1、b2、d1 共 4 条记录");
        assertEquals(3, records.stream().filter(FlowRecord::isTodo).count());
        assertEquals(0, recordsOfNode(records, singleCNode).size(), "else 分支不应产生记录");

        // 分支1：b1 或签办理 -> b2 自动已审核；分支2 未完成，流程不结束
        pass(b1Todo, anyNode, b1, data);
        assertNoTodo(b2);
        todoOf(d1);

        records = processRecords(d1Todo);
        assertEquals(4, records.size());
        assertEquals(2, recordsOfNode(records, anyNode).stream().filter(FlowRecord::isDone).count());
        assertEquals(1, records.stream().filter(FlowRecord::isTodo).count(), "仅 d1 一条待办");
        assertEquals(0, records.stream().filter(FlowRecord::isFinish).count(),
                "分支2 未完成，流程不应结束");

        // processNodes 视图：并行分支顺序不固定，按节点名称核实
        List<ProcessNode> nodeList = processNodes(d1Todo, d1, data);
        assertEquals(4, nodeList.size());
        assertEquals(ProcessNode.ApproveState.PASS,
                nodeByName(nodeList, startNode.getName()).getApproveState());
        ProcessNode anyNodeView = nodeByName(nodeList, anyNode.getName());
        assertEquals(ProcessNode.ApproveState.PASS, anyNodeView.getApproveState());
        assertEquals(2, anyNodeView.getOperators().size(), "或签分支视图应展示 b1、b2 两条记录");
        ProcessNode dNodeView = nodeByName(nodeList, singleDNode.getName());
        assertEquals(ProcessNode.ApproveState.PROCESSING, dNodeView.getApproveState());
        assertEquals(1, dNodeView.getOperators().size());
        assertEquals(ProcessNode.ApproveState.PENDING,
                nodeByName(nodeList, endNode.getName()).getApproveState());
        assertEquals(2, nodeList.stream().filter(ProcessNode::isHistory).count());

        // 分支2：d1 办理 -> 并行汇聚完成，流程结束
        pass(d1Todo, singleDNode, d1, data);
        records = processRecords(d1Todo);
        assertEquals(4, records.size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).count());
        assertEquals(4, records.stream().filter(FlowRecord::isDone).count());
        assertEquals(0, records.stream().filter(FlowRecord::isTodo).count());
        assertNoTodo(b1);
        assertNoTodo(b2);
        assertNoTodo(c1);
        assertNoTodo(d1);

        // 流程结束后视图：全部节点通过
        nodeList = processNodes(d1Todo, d1, data);
        assertEquals(4, nodeList.size());
        assertEquals(4, nodeList.stream().filter(ProcessNode::isHistory).count());
    }
}

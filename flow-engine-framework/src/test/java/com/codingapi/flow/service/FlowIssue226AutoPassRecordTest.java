package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.PassAction;
import com.codingapi.flow.action.actions.RejectAction;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.node.nodes.SubProcessNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowProcessNodeRequest;
import com.codingapi.flow.pojo.request.FlowRevokeRequest;
import com.codingapi.flow.pojo.response.ProcessNode;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.MultiOperatorAuditStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.RevokeStrategy;
import com.codingapi.flow.strategy.node.SameOperatorAuditStrategy;
import com.codingapi.flow.strategy.node.SubProcessStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * issue #226：相同流程审批人自动通过后，B 节点流程记录缺失
 *
 * <p>流程：A(发起) -> B(审批，审批人 a，依次审批 + 相同人员自动审批) -> C(审批，审批人 c) -> D(结束)。
 *
 * <p>a 发起流程后，B 节点因审批人与提交人一致被自动通过，流程直达 C 节点由 c 审批。
 * 此时流程记录与节点展示中应能看到 B 节点的自动通过记录（共 3 条记录：A 已办、B 自动通过、C 待办），
 * 但当前实现中 B 节点不产生任何记录（见 {@code BaseAuditNode#generateCurrentRecords} 自动通过分支
 * 直接返回 {@code generateNextNodeRecords}），导致节点展示只剩 A-C-D。
 */
class FlowIssue226AutoPassRecordTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    // 人员：a=1 发起人（同时是 B 节点审批人），c=3 C 节点审批人
    private static final long A = 1L;
    private static final long C = 3L;

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
                .build();
    }

    private Map<String, Object> data() {
        return Map.of("name", "lorne", "days", 1);
    }

    private StartNode startNode() {
        return StartNode.builder().build();
    }

    /**
     * 构建审批节点（脚本动态加载审批人）
     */
    private ApprovalNode approvalNode(String name, String operatorIds,
                                      SameOperatorAuditStrategy.Type sameOperatorType,
                                      MultiOperatorAuditStrategy auditStrategy) {
        NodeStrategyBuilder strategyBuilder = NodeStrategyBuilder.builder()
                .addStrategy(new OperatorLoadStrategy(
                        FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return " + operatorIds + "}").getKey()));
        if (sameOperatorType != null) {
            strategyBuilder.addStrategy(new SameOperatorAuditStrategy(sameOperatorType));
        }
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
     * 发起人提交开始节点（直接通过），返回 B 自动通过后 c 在 C 节点的待办
     */
    private FlowRecord submitStart(Workflow workflow, StartNode startNode, User user) {
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data());
        createRequest.setActionId(startNode.actionManager().getActions().get(0).id());
        createRequest.setOperatorId(user.getUserId());
        factory.flowService.create(createRequest);

        FlowRecord todo = todoOf(user);
        FlowActionRequest request = new FlowActionRequest();
        request.setFormData(data());
        request.setRecordId(todo.getId());
        request.setAdvice(new FlowAdviceBody(startNode.actionManager().getActions().get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(request);
        return todo;
    }

    private FlowRecord todoOf(User user) {
        List<FlowRecord> list = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, list.size(), user.getName() + " 应有且仅有一条待办");
        return list.get(0);
    }

    private List<FlowRecord> processRecords(FlowRecord anyRecord) {
        return factory.flowRecordRepository.findProcessRecords(anyRecord.getProcessId());
    }

    private IFlowAction passAction(IFlowNode node) {
        return node.actionManager().getActions().stream()
                .filter(action -> "PASS".equals(action.type()))
                .findFirst()
                .orElseThrow();
    }

    /**
     * 以指定动作执行一条待办记录
     */
    private void runAction(FlowRecord todo, IFlowAction action, User operator) {
        FlowActionRequest request = new FlowActionRequest();
        request.setFormData(data());
        request.setRecordId(todo.getId());
        request.setAdvice(new FlowAdviceBody(action.id(), "处理", operator.getUserId()));
        factory.flowService.action(request);
    }

    // ==================== issue #226 场景 ====================

    /**
     * 场景：B 节点审批人仅为 a（与提交人一致）+ 相同人员自动审批（AUTO_PASS）+ 依次审批，
     * C 节点审批人为 c。a 发起流程后流程直达 C 节点。
     * <p>期望（issue #226）：
     * <ul>
     *     <li>B 节点应产生一条自动通过的流程记录（已办），流程记录共 3 条：A 已办、B 自动通过、C 待办；</li>
     *     <li>节点展示可见 A、B、C、D 四个节点，B 节点状态为已通过。</li>
     * </ul>
     */
    @Test
    void autoPassedNodeShouldKeepFlowRecord() {
        User a = new User(A, "a");
        User c = new User(C, "c");
        registerUsers(a, c);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[1]",
                SameOperatorAuditStrategy.Type.AUTO_PASS,
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.SEQUENCE, 0));
        ApprovalNode cNode = approvalNode("C审批", "[3]", null, null);
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(a, startNode, bNode, cNode, endNode);
        submitStart(workflow, startNode, a);

        // a 无待办（B 节点被自动通过），c 在 C 节点收到待办
        assertEquals(0, factory.flowRecordRepository.findTodoByOperator(a.getUserId()).size(), "a 不应有待办");
        FlowRecord cTodo = todoOf(c);
        assertEquals(cNode.getId(), cTodo.getNodeId(), "流程应直达 C 节点");

        // 流程记录应存在 3 条：A 已办、B 自动通过、C 待办
        List<FlowRecord> records = processRecords(cTodo);
        assertEquals(3, records.size(), "流程记录应含 A、B、C 三条，B 节点自动通过后应保留记录");

        // B 节点应有一条已办记录
        List<FlowRecord> bRecords = records.stream()
                .filter(r -> bNode.getId().equals(r.getNodeId()))
                .toList();
        assertEquals(1, bRecords.size(), "B 节点应存在一条自动通过的流程记录");
        assertTrue(bRecords.get(0).isDone(), "B 节点记录应为已办状态");

        // 节点展示可见 A、B、C、D 四个节点
        List<ProcessNode> processNodes = factory.flowService.processNodes(
                new FlowProcessNodeRequest(cTodo.getId(), c.getUserId(), data()));
        assertEquals(4, processNodes.size(), "流程节点展示应含 A、B、C、D 四个节点");
        assertEquals(bNode.getId(), processNodes.get(1).getNodeId(), "第二个节点应为 B 审批节点");
        assertEquals(ProcessNode.ApproveState.PASS, processNodes.get(1).getApproveState(),
                "B 节点自动通过后展示状态应为已通过");
    }

    /**
     * 边界场景：自动通过节点的下游直接是结束节点（A -> B(自动通过) -> End）。
     * <p>a 发起并处理开始节点后，B 节点自动通过并保留已办记录，流程应正常判定结束，
     * 全部记录（开始、B）均为完成状态，且无人持有待办。
     */
    @Test
    void autoPassDirectBeforeEndNodeShouldFinishFlow() {
        User a = new User(A, "a");
        registerUsers(a);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[1]",
                SameOperatorAuditStrategy.Type.AUTO_PASS, null);
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(a, startNode, bNode, endNode);
        FlowRecord startRecord = submitStart(workflow, startNode, a);

        // 流程无人待办，直接结束
        assertEquals(0, factory.flowRecordRepository.findTodoByOperator(a.getUserId()).size(), "a 不应有待办");

        // 流程记录：开始 + B 自动通过 共 2 条（结束节点虚拟记录不持久化），全部完成
        List<FlowRecord> records = processRecords(startRecord);
        assertEquals(2, records.size(), "流程记录应含 开始、B 两条");
        assertEquals(1, records.stream().filter(r -> bNode.getId().equals(r.getNodeId())).count(),
                "B 节点应保留一条自动通过的流程记录");
        assertTrue(records.stream().allMatch(FlowRecord::isFinish),
                "自动通过直达结束节点后，全部流程记录应为完成状态");
    }

    /**
     * 回归场景（issue #226 修复 v2）：REVOKE_NEXT 撤回时自动通过留痕记录应视为透明节点。
     * <p>流程：A(发起，REVOKE_NEXT 撤回) -> B(审批人 a，AUTO_PASS) -> C(审批人 c) -> D(结束)。
     * a 提交后 B 被自动通过、流程停在 C 待办。此时撤回 A 的记录，
     * B 的自动通过留痕（已办、无真实审批）不应让"下级已办"误判成立，
     * 穿透后应发现 C 仍为待办并放行撤回：a 回到发起待办，c 待办被撤销。
     */
    @Test
    void revokeNextThroughAutoPassRecordShouldSucceed() {
        User a = new User(A, "a");
        User c = new User(C, "c");
        registerUsers(a, c);

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new RevokeStrategy(true, RevokeStrategy.Type.REVOKE_NEXT))
                        .build())
                .build();
        ApprovalNode bNode = approvalNode("B审批", "[1]",
                SameOperatorAuditStrategy.Type.AUTO_PASS, null);
        ApprovalNode cNode = approvalNode("C审批", "[3]", null, null);
        EndNode endNode = EndNode.builder().build();

        Workflow workflow = saveWorkflow(a, startNode, bNode, cNode, endNode);
        FlowRecord startRecord = submitStart(workflow, startNode, a);

        FlowRecord cTodo = todoOf(c);
        assertEquals(cNode.getId(), cTodo.getNodeId());

        // 撤回发起记录：修复前被 B 的自动通过留痕误判为"下级已办"而拒绝，修复后穿透放行
        factory.flowService.revoke(new FlowRevokeRequest(startRecord.getId(), a.getUserId()));

        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(a.getUserId()).size(),
                "撤回成功后 a 应回到发起待办");
        assertEquals(0, factory.flowRecordRepository.findTodoByOperator(c.getUserId()).size(),
                "撤回应连同下游 C 待办一并撤销");
    }

    /**
     * 回归场景（issue #226 修复 v2）：自动通过的留痕记录不支持撤销。
     * <p>留痕记录从未产生待办、未发生真实审批，不存在"撤回自己办理的审批"语义，
     * 对其发起撤销应抛出 FlowStateException（服务端权威拦截，与 FlowContent 入口隐藏一致）。
     */
    @Test
    void revokeAutoPassRecordShouldThrow() {
        User a = new User(A, "a");
        User c = new User(C, "c");
        registerUsers(a, c);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[1]",
                SameOperatorAuditStrategy.Type.AUTO_PASS, null);
        ApprovalNode cNode = approvalNode("C审批", "[3]", null, null);

        Workflow workflow = saveWorkflow(a, startNode, bNode, cNode, EndNode.builder().build());
        FlowRecord startRecord = submitStart(workflow, startNode, a);
        FlowRecord cTodo = todoOf(c);

        FlowRecord bRecord = processRecords(cTodo).stream()
                .filter(r -> bNode.getId().equals(r.getNodeId()))
                .findFirst().orElseThrow();
        assertTrue(bRecord.isAutoDone(), "B 节点记录应为自动办结留痕");

        assertThrows(FlowStateException.class,
                () -> factory.flowService.revoke(new FlowRevokeRequest(bRecord.getId(), a.getUserId())),
                "自动通过留痕记录不应支持撤销");
        assertEquals(startRecord.getProcessId(), cTodo.getProcessId());
    }

    /**
     * 回归场景（issue #226 修复 v2）：自动通过守卫限定为 PASS 动作。
     * <p>流程：A(发起) -> D(审批人 a，拒绝动作指向 X) -> X(审批人 a，AUTO_PASS) -> E(结束)。
     * 提交人 a 在 D 节点执行拒绝并回退到 X 节点时，流转动作是 REJECT 而非 PASS，
     * X 节点应生成正常待办交提交人重新处理，而不是被静默自动通过
     * （修复前拒绝/退回流转也会命中相同人员自动通过分支，产生错误留痕并继续向下推进）。
     */
    @Test
    void rejectToAutoPassNodeBySubmitterShouldCreateNormalTodo() {
        User a = new User(A, "a");
        registerUsers(a);

        StartNode startNode = startNode();
        // 先构建 X 以取得节点 id 供拒绝脚本引用
        ApprovalNode xNode = approvalNode("X审批", "[1]",
                SameOperatorAuditStrategy.Type.AUTO_PASS, null);
        RejectAction rejectAction = RejectAction.defaultAction();
        rejectAction.setScript(FlowGroovyScriptFactory
                .createActionRejectScript("def run(request){return '" + xNode.getId() + "'}")
                .getKey());
        ApprovalNode dNode = ApprovalNode.builder()
                .name("D审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [1]}").getKey()))
                        .build())
                .actions(List.of(PassAction.defaultAction(), rejectAction))
                .build();

        Workflow workflow = saveWorkflow(a, startNode, dNode, xNode, EndNode.builder().build());
        submitStart(workflow, startNode, a);

        // D 节点审批人为 a（未开启自动通过），a 在 D 待办上执行拒绝，回退到 X
        FlowRecord dTodo = todoOf(a);
        assertEquals(dNode.getId(), dTodo.getNodeId());
        runAction(dTodo, rejectAction, a);

        // X 节点应生成真实待办（REJECT 不触发自动通过），而非被静默跳过直达结束
        FlowRecord xTodo = todoOf(a);
        assertEquals(xNode.getId(), xTodo.getNodeId(), "拒绝回退到自动通过节点应生成正常待办");
        assertTrue(xTodo.isTodo(), "X 节点记录应为待办状态");
    }

    /**
     * 回归场景（issue #226 修复 v2）：主流程经自动通过留痕记录进入子流程后，
     * 子流程完成时应能以通过动作兜底恢复主流程，而非因留痕记录无审批动作（actionId=null）NPE 停滞。
     * <p>主流程：A(发起 a) -> M(审批人 a，AUTO_PASS) -> 子流程(子审批人 c) -> 结束。
     * a 提交后 M 自动通过、子流程创建；c 完成子流程审批后主流程应恢复并结束。
     */
    @Test
    void autoPassBeforeSubProcessShouldResumeParentOnChildCompletion() {
        User a = new User(A, "a");
        User c = new User(C, "c");
        registerUsers(a, c);

        // 子流程：开始 -> 子审批(c) -> 结束
        StartNode childStart = startNode();
        ApprovalNode childApproval = approvalNode("子审批", "[3]", null, null);
        Workflow childWorkflow = WorkflowBuilder.builder()
                .title("子流程")
                .code("child-leave")
                .createdOperator(a)
                .form(form())
                .addNode(childStart)
                .addNode(childApproval)
                .addNode(EndNode.builder().build())
                .build();
        factory.workflowService.saveWorkflow(childWorkflow);

        // 主流程：A -> M(自动通过) -> 子流程 -> 结束
        String createScript = "def run(request){return request.toCreateRequest('child-leave', "
                + a.getUserId() + ", '" + passAction(childStart).id() + "', [name:'lorne', days:1])}";
        SubProcessNode subProcessNode = SubProcessNode.builder()
                .name("子流程")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy(
                                FlowGroovyScriptFactory.createSubProcessScript(createScript).getKey(), true))
                        .build())
                .build();
        ApprovalNode mNode = approvalNode("M审批", "[1]",
                SameOperatorAuditStrategy.Type.AUTO_PASS, null);
        StartNode parentStart = startNode();

        Workflow workflow = saveWorkflow(a, parentStart, mNode, subProcessNode, EndNode.builder().build());
        FlowRecord startRecord = submitStart(workflow, parentStart, a);

        // M 自动通过后子流程被创建，c 收到子流程审批待办
        FlowRecord childTodo = todoOf(c);
        assertEquals(childApproval.getId(), childTodo.getNodeId(), "子流程应已创建并流转至子审批节点");

        // c 完成子流程：恢复主流程的前驱记录为 M 的自动通过留痕（actionId=null），应以通过动作兜底
        runAction(childTodo, passAction(childApproval), c);

        // 主流程恢复后直达结束：无人持有待办，主流程记录全部完成
        assertEquals(0, factory.flowRecordRepository.findTodoByOperator(a.getUserId()).size(), "a 不应有待办");
        assertEquals(0, factory.flowRecordRepository.findTodoByOperator(c.getUserId()).size(), "c 不应有待办");
        List<FlowRecord> parentRecords = processRecords(startRecord);
        assertTrue(parentRecords.stream().allMatch(FlowRecord::isFinish),
                "子流程完成后主流程应恢复并结束，全部主流程记录应为完成状态");
    }
}

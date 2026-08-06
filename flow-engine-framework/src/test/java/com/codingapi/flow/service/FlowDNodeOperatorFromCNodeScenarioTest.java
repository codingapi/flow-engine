package com.codingapi.flow.service;

import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.RejectAction;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * D 节点审批人跟随 C 节点审批人的场景测试。
 *
 * <p>流程设计：A(发起) -> B(发起人指定) -> C(审批人指定) -> D(与C同审批人) -> E(结束)。
 * <ul>
 *     <li>B 节点：发起人设定（INITIATOR_SELECT），发起人 a 指定审批人 b</li>
 *     <li>C 节点：审批人设定（APPROVER_SELECT），b 审批时指定审批人 c1、c2，并签（均需审批）</li>
 *     <li>D 节点：脚本模式（SCRIPT），通过 currentRecord 查询其所在节点的流程记录，
 *     将 C 节点的审批人员作为 D 节点的审批人员，并签（均需审批）</li>
 * </ul>
 *
 * <p>验证点：
 * <ol>
 *     <li>正常场景与回退场景（D 节点 c2 拒绝退回 C 节点）下流程是否可以正常走完</li>
 *     <li>在 B 节点查看流程记录时 C、D 节点审批人的展示是否正确</li>
 * </ol>
 */
class FlowDNodeOperatorFromCNodeScenarioTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    private User a;
    private User b;
    private User c1;
    private User c2;

    private StartNode startNode;
    private ApprovalNode bNode;
    private ApprovalNode cNode;
    private ApprovalNode dNode;
    private EndNode endNode;
    private Workflow workflow;

    private Map<String, Object> data;

    @BeforeEach
    void setUp() {
        a = new User(1, "a");
        b = new User(2, "b");
        c1 = new User(3, "c1");
        c2 = new User(4, "c2");
        factory.userGateway.save(a);
        factory.userGateway.save(b);
        factory.userGateway.save(c1);
        factory.userGateway.save(c2);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("测试流程")
                .code("test-flow")
                .addField("标题", "title", DataType.STRING)
                .build();

        // A：发起节点
        startNode = StartNode.builder().build();

        // B：发起人指定审批人
        bNode = ApprovalNode.builder()
                .name("B审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(OperatorLoadStrategy.initiatorSelectStrategy())
                        .build())
                .build();

        // C：审批人指定审批人，c1、c2 并签（均需审批）
        cNode = ApprovalNode.builder()
                .name("C审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(OperatorLoadStrategy.approverSelectStrategy())
                        .addStrategy(new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.MERGE, 1.0f))
                        .build())
                .build();

        // D：审批人与 C 节点保持一致，通过 currentRecord 查询 C 节点的审批人，c1、c2 并签（均需审批）
        dNode = ApprovalNode.builder()
                .name("D审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(dNodeOperatorScript()).getKey()))
                        .addStrategy(new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.MERGE, 1.0f))
                        .build())
                .build();
        // D 节点拒绝时退回到 C 节点
        RejectAction rejectAction = (RejectAction) dNode.actionManager().getActionByType(ActionType.REJECT.name());
        rejectAction.setScript(FlowGroovyScriptFactory.createActionRejectScript(
                "def run(request){return '" + cNode.getId() + "'}").getKey());

        // E：结束节点
        endNode = EndNode.builder().build();

        workflow = WorkflowBuilder.builder()
                .title("测试流程")
                .code("test-flow")
                .createdOperator(a)
                .form(form)
                .addNode(startNode)
                .addNode(bNode)
                .addNode(cNode)
                .addNode(dNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        data = Map.of("title", "test");
    }

    /**
     * D 节点审批人脚本：直接复用上一节点（C 节点）的审批人员。
     * 取数逻辑由 {@code FlowSession#loadPreviousNodeOperatorIds()} 提供。
     */
    private String dNodeOperatorScript() {
        return """
                def run(request) {
                    return request.findPreviousNodeOperatorIds()
                }
                """;
    }

    /**
     * a 发起流程并为 B 节点指定审批人 b，随后 a 提交开始节点，流程流转到 B 节点。
     */
    private FlowRecord startFlowToBNode() {
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(a.getUserId());
        createRequest.setOperatorSelectMap(Map.of(bNode.getId(), List.of(b.getUserId())));
        factory.flowService.create(createRequest);

        List<FlowRecord> aTodos = factory.flowRecordRepository.findTodoByOperator(a.getUserId());
        assertEquals(1, aTodos.size());

        FlowActionRequest aAction = new FlowActionRequest();
        aAction.setFormData(data);
        aAction.setRecordId(aTodos.get(0).getId());
        FlowAdviceBody aAdvice = new FlowAdviceBody(startActions.get(0).id(), "提交", a.getUserId());
        aAdvice.setOperatorSelectMap(Map.of(bNode.getId(), List.of(b.getUserId())));
        aAction.setAdvice(aAdvice);
        factory.flowService.action(aAction);

        List<FlowRecord> bTodos = factory.flowRecordRepository.findTodoByOperator(b.getUserId());
        assertEquals(1, bTodos.size());
        assertEquals(bNode.getId(), bTodos.get(0).getNodeId());
        return bTodos.get(0);
    }

    /**
     * b 审批 B 节点并为 C 节点指定审批人 c1、c2，流程流转到 C 节点（c1、c2 均收到待办）。
     */
    private void passBNodeToCNode(FlowRecord bTodo) {
        List<IFlowAction> bActions = bNode.actionManager().getActions();

        FlowActionRequest bAction = new FlowActionRequest();
        bAction.setFormData(data);
        bAction.setRecordId(bTodo.getId());
        FlowAdviceBody bAdvice = new FlowAdviceBody(bActions.get(0).id(), "同意", b.getUserId());
        bAdvice.setOperatorSelectMap(Map.of(cNode.getId(), List.of(c1.getUserId(), c2.getUserId())));
        bAction.setAdvice(bAdvice);
        factory.flowService.action(bAction);

        List<FlowRecord> c1Todos = factory.flowRecordRepository.findTodoByOperator(c1.getUserId());
        assertEquals(1, c1Todos.size());
        assertEquals(cNode.getId(), c1Todos.get(0).getNodeId());
        List<FlowRecord> c2Todos = factory.flowRecordRepository.findTodoByOperator(c2.getUserId());
        assertEquals(1, c2Todos.size());
        assertEquals(cNode.getId(), c2Todos.get(0).getNodeId());
    }

    /**
     * c1、c2 审批 C 节点（并签，均需审批），流程流转到 D 节点，
     * 验证 D 节点审批人与 C 节点一致（c1、c2 均收到待办）。
     */
    private void passCNodeToDNode() {
        List<IFlowAction> cActions = cNode.actionManager().getActions();

        // c1 审批 C 节点（并签未全部完成，流程仍停留在 C 节点）
        List<FlowRecord> c1Todos = factory.flowRecordRepository.findTodoByOperator(c1.getUserId());
        FlowActionRequest c1Action = new FlowActionRequest();
        c1Action.setFormData(data);
        c1Action.setRecordId(c1Todos.get(0).getId());
        c1Action.setAdvice(new FlowAdviceBody(cActions.get(0).id(), "同意", c1.getUserId()));
        factory.flowService.action(c1Action);
        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(c2.getUserId()).size(),
                "c1 审批后并签未完成，c2 仍应有 C 节点待办");

        // c2 审批 C 节点，C 节点完成，流程流转到 D 节点
        List<FlowRecord> c2Todos = factory.flowRecordRepository.findTodoByOperator(c2.getUserId());
        FlowActionRequest c2Action = new FlowActionRequest();
        c2Action.setFormData(data);
        c2Action.setRecordId(c2Todos.get(0).getId());
        c2Action.setAdvice(new FlowAdviceBody(cActions.get(0).id(), "同意", c2.getUserId()));
        factory.flowService.action(c2Action);

        // D 节点审批人应与 C 节点一致：c1、c2 均收到 D 节点待办
        List<FlowRecord> d1Todos = factory.flowRecordRepository.findTodoByOperator(c1.getUserId());
        assertEquals(1, d1Todos.size());
        assertEquals(dNode.getId(), d1Todos.get(0).getNodeId());
        List<FlowRecord> d2Todos = factory.flowRecordRepository.findTodoByOperator(c2.getUserId());
        assertEquals(1, d2Todos.size());
        assertEquals(dNode.getId(), d2Todos.get(0).getNodeId());
    }

    /**
     * c1、c2 审批 D 节点（并签，均需审批），流程结束。
     */
    private void passDNodeToEnd() {
        List<IFlowAction> dActions = dNode.actionManager().getActions();

        // c1 审批 D 节点（并签未全部完成，流程仍停留在 D 节点）
        List<FlowRecord> d1Todos = factory.flowRecordRepository.findTodoByOperator(c1.getUserId());
        FlowActionRequest d1Action = new FlowActionRequest();
        d1Action.setFormData(data);
        d1Action.setRecordId(d1Todos.get(0).getId());
        d1Action.setAdvice(new FlowAdviceBody(dActions.get(0).id(), "同意", c1.getUserId()));
        factory.flowService.action(d1Action);
        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(c2.getUserId()).size(),
                "c1 审批后并签未完成，c2 仍应有 D 节点待办");

        // c2 审批 D 节点，流程结束
        List<FlowRecord> d2Todos = factory.flowRecordRepository.findTodoByOperator(c2.getUserId());
        FlowActionRequest d2Action = new FlowActionRequest();
        d2Action.setFormData(data);
        d2Action.setRecordId(d2Todos.get(0).getId());
        d2Action.setAdvice(new FlowAdviceBody(dActions.get(0).id(), "同意", c2.getUserId()));
        factory.flowService.action(d2Action);
    }

    /**
     * 正常场景：
     * a 发起并指定 B 审批人为 b；b 提交并指定 C 审批人为 c1、c2；
     * c1、c2 审批 C 后流程到达 D 节点，D 节点审批人仍为 c1、c2；
     * c1、c2 审批 D 后流程结束。
     */
    @Test
    void normalScenario_shouldFinishFlow_whenDOperatorFollowsCNode() {
        // a 发起 -> B(b)
        startFlowToBNode();

        // b 提交 -> C(c1、c2)
        passBNodeToCNode(factory.flowRecordRepository.findTodoByOperator(b.getUserId()).get(0));

        // c1、c2 审批 C -> D(c1、c2)
        passCNodeToDNode();

        // c1、c2 审批 D -> 流程结束
        String processId = factory.flowRecordRepository.findTodoByOperator(c1.getUserId()).get(0).getProcessId();
        passDNodeToEnd();

        // 验证 D 节点的审批人与 C 节点一致
        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(processId);
        List<FlowRecord> dRecords = records.stream()
                .filter(record -> dNode.getId().equals(record.getNodeId()))
                .toList();
        assertEquals(2, dRecords.size());
        assertTrue(dRecords.stream().anyMatch(record -> record.getCurrentOperatorId() == c1.getUserId()));
        assertTrue(dRecords.stream().anyMatch(record -> record.getCurrentOperatorId() == c2.getUserId()));
    }

    /**
     * 回退场景：
     * a 发起并指定 B 审批人为 b；b 提交并指定 C 审批人为 c1、c2；
     * c1、c2 审批 C 后流程到达 D 节点；c1 审批 D 节点，c2 拒绝，流程退回 C 节点；
     * C 节点重新审批后再次到达 D 节点；c1、c2 审批 D 后流程结束。
     */
    @Test
    void rejectScenario_shouldReturnToCNode_whenC2RejectAtDNode() {
        // a 发起 -> B(b)
        startFlowToBNode();

        // b 提交 -> C(c1、c2)
        passBNodeToCNode(factory.flowRecordRepository.findTodoByOperator(b.getUserId()).get(0));

        // c1、c2 审批 C -> D(c1、c2)
        passCNodeToDNode();

        List<IFlowAction> dActions = dNode.actionManager().getActions();
        String processId = factory.flowRecordRepository.findTodoByOperator(c1.getUserId()).get(0).getProcessId();

        // c1 审批 D 节点
        List<FlowRecord> d1Todos = factory.flowRecordRepository.findTodoByOperator(c1.getUserId());
        FlowActionRequest d1Action = new FlowActionRequest();
        d1Action.setFormData(data);
        d1Action.setRecordId(d1Todos.get(0).getId());
        d1Action.setAdvice(new FlowAdviceBody(dActions.get(0).id(), "同意", c1.getUserId()));
        factory.flowService.action(d1Action);

        // c2 拒绝 D 节点，流程退回 C 节点
        List<FlowRecord> d2Todos = factory.flowRecordRepository.findTodoByOperator(c2.getUserId());
        assertEquals(1, d2Todos.size());
        assertEquals(dNode.getId(), d2Todos.get(0).getNodeId());

        IFlowAction rejectAction = dNode.actionManager().getActionByType(ActionType.REJECT.name());
        FlowActionRequest rejectRequest = new FlowActionRequest();
        rejectRequest.setFormData(data);
        rejectRequest.setRecordId(d2Todos.get(0).getId());
        rejectRequest.setAdvice(new FlowAdviceBody(rejectAction.id(), "不同意，退回C节点", c2.getUserId()));
        factory.flowService.action(rejectRequest);

        // 验证：D 节点待办清空，流程退回 C 节点，c1、c2 重新收到 C 节点待办
        assertEquals(0, factory.flowRecordRepository.findTodoByOperator(c1.getUserId()).stream()
                .filter(record -> dNode.getId().equals(record.getNodeId())).count(),
                "拒绝后 c1 不应再有 D 节点待办");
        List<FlowRecord> c1RoundTwoTodos = factory.flowRecordRepository.findTodoByOperator(c1.getUserId());
        assertEquals(1, c1RoundTwoTodos.size(), "拒绝退回后 c1 应重新收到 C 节点待办");
        assertEquals(cNode.getId(), c1RoundTwoTodos.get(0).getNodeId());
        List<FlowRecord> c2RoundTwoTodos = factory.flowRecordRepository.findTodoByOperator(c2.getUserId());
        assertEquals(1, c2RoundTwoTodos.size(), "拒绝退回后 c2 应重新收到 C 节点待办");
        assertEquals(cNode.getId(), c2RoundTwoTodos.get(0).getNodeId());

        // C 节点重新审批，流程再次到达 D 节点（沿用原流程实例）
        passCNodeToDNode();
        assertEquals(processId, factory.flowRecordRepository.findTodoByOperator(c1.getUserId()).get(0).getProcessId(),
                "退回后应沿用原流程实例");

        // c1、c2 审批 D 节点，流程结束
        passDNodeToEnd();

        // 验证两轮 D 节点的审批人均为 c1、c2
        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(processId);
        assertEquals(10, records.size(), "应包含两轮 C、D 审批记录共 10 条");
        List<FlowRecord> dRecords = records.stream()
                .filter(record -> dNode.getId().equals(record.getNodeId()))
                .toList();
        assertEquals(4, dRecords.size(), "两轮 D 节点应各生成 c1、c2 两条记录");
        assertEquals(2, dRecords.stream().filter(record -> record.getCurrentOperatorId() == c1.getUserId()).count());
        assertEquals(2, dRecords.stream().filter(record -> record.getCurrentOperatorId() == c2.getUserId()).count());
    }

    /**
     * 验证点 2：流程停留在 B 节点时（b 尚未指定 C 节点审批人），查看流程记录。
     *
     * <p>期望：
     * <ul>
     *     <li>C 节点展示为“审批人设定”模式（APPROVER_SELECT），无具体审批人</li>
     *     <li>D 节点不应展示错误的审批人（此时 C 节点尚未执行，D 节点审批人无法确定，
     *     尤其不能错误地展示为 B 节点的审批人 b）</li>
     * </ul>
     */
    @Test
    void processNodes_shouldNotShowWrongDOperator_whenFlowAtBNode() {
        // a 发起 -> B(b)，此时 b 尚未提交
        FlowRecord bTodo = startFlowToBNode();

        List<ProcessNode> nodeList = factory.flowService.processNodes(
                new FlowProcessNodeRequest(String.valueOf(bTodo.getId()), b.getUserId(), data));

        // 节点顺序：开始 -> B(审批中) -> C(待审批) -> D(待审批) -> 结束(待审批)
        assertEquals(5, nodeList.size());
        assertEquals(startNode.getId(), nodeList.get(0).getNodeId());
        assertEquals(bNode.getId(), nodeList.get(1).getNodeId());
        assertEquals(cNode.getId(), nodeList.get(2).getNodeId());
        assertEquals(dNode.getId(), nodeList.get(3).getNodeId());
        assertEquals(endNode.getId(), nodeList.get(4).getNodeId());
        assertEquals(ProcessNode.ApproveState.PROCESSING, nodeList.get(1).getApproveState());

        // C 节点：尚未指定审批人，应展示为“审批人设定”模式
        ProcessNode cPreview = nodeList.get(2);
        assertEquals(ProcessNode.ApproveState.PENDING, cPreview.getApproveState());
        assertEquals(ProcessNode.OperatorStrategy.APPROVER_SELECT, cPreview.getOperatorStrategy());

        // D 节点：C 节点尚未执行，D 节点审批人无法确定，不应展示任何审批人，
        // 尤其不能错误地展示为 B 节点的审批人 b
        ProcessNode dPreview = nodeList.get(3);
        assertEquals(ProcessNode.ApproveState.PENDING, dPreview.getApproveState());
        assertTrue(dPreview.getOperators() == null || dPreview.getOperators().isEmpty(),
                "C 节点未执行前，D 节点预览不应展示审批人");
    }

    /**
     * 验证点 2：流程停留在 C 节点时（b 已指定 C 节点审批人为 c1、c2），查看流程记录。
     *
     * <p>期望：
     * <ul>
     *     <li>C 节点展示审批人 c1、c2</li>
     *     <li>D 节点预览应展示与 C 节点一致的审批人 c1、c2</li>
     * </ul>
     */
    @Test
    void processNodes_shouldShowDOperatorSameAsC_whenFlowAtCNode() {
        // a 发起 -> B(b) -> C(c1、c2)
        startFlowToBNode();
        passBNodeToCNode(factory.flowRecordRepository.findTodoByOperator(b.getUserId()).get(0));

        FlowRecord c1Todo = factory.flowRecordRepository.findTodoByOperator(c1.getUserId()).get(0);
        List<ProcessNode> nodeList = factory.flowService.processNodes(
                new FlowProcessNodeRequest(String.valueOf(c1Todo.getId()), c1.getUserId(), data));

        // C 节点：审批中，展示审批人 c1、c2
        ProcessNode cNodeView = nodeList.stream()
                .filter(node -> cNode.getId().equals(node.getNodeId()))
                .findFirst()
                .orElse(null);
        assertNotNull(cNodeView);
        assertEquals(ProcessNode.ApproveState.PROCESSING, cNodeView.getApproveState());
        assertEquals(2, cNodeView.getOperators().size());
        assertTrue(cNodeView.getOperators().stream()
                .anyMatch(operator -> operator.getFlowOperator().getUserId() == c1.getUserId()));
        assertTrue(cNodeView.getOperators().stream()
                .anyMatch(operator -> operator.getFlowOperator().getUserId() == c2.getUserId()));

        // D 节点：预览应展示与 C 节点一致的审批人 c1、c2
        ProcessNode dNodeView = nodeList.stream()
                .filter(node -> dNode.getId().equals(node.getNodeId()))
                .findFirst()
                .orElse(null);
        assertNotNull(dNodeView);
        assertEquals(ProcessNode.ApproveState.PENDING, dNodeView.getApproveState());
        assertNotNull(dNodeView.getOperators(), "C 节点审批人已确定后，D 节点预览应展示审批人");
        assertEquals(2, dNodeView.getOperators().size(), "D 节点审批人应与 C 节点一致");
        assertTrue(dNodeView.getOperators().stream()
                .anyMatch(operator -> operator.getFlowOperator().getUserId() == c1.getUserId()));
        assertTrue(dNodeView.getOperators().stream()
                .anyMatch(operator -> operator.getFlowOperator().getUserId() == c2.getUserId()));
        // D 节点预览不能展示为 B 节点的审批人 b
        assertFalse(dNodeView.getOperators().stream()
                        .anyMatch(operator -> operator.getFlowOperator().getUserId() == b.getUserId()),
                "D 节点审批人不应错误地展示为 B 节点的审批人");
    }
}

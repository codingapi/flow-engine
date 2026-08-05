package com.codingapi.flow.service;

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
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowParallelJoinServiceTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    /**
     * 测试目标：验证 A 提交后进入 B、C、D 三条并行分支，每条分支顺序审批两级，
     * 三条分支全部完成后只汇聚一次到 E，E 审批后流程在 F 结束。
     *
     * <p>流程结构：A -> parallel(B1 -> B2, C1 -> C2, D1 -> D2) -> E -> F。</p>
     * <p>前置条件：每个审批节点配置独立审批人，避免多人待办合并影响并行汇聚判断。</p>
     * <p>执行步骤：A 提交，按 B1/B2、C1/C2、D1/D2 的顺序完成三条分支，最后审批 E。</p>
     * <p>期望结果：E 在最后一条分支完成前不存在，之后只创建一次；E 通过后无待办且流程完成。</p>
     */
    @Test
    void shouldJoinThreeTwoLevelParallelBranchesBeforeEnteringE() {
        User initiator = saveUser(1, "A发起人");
        User b1Operator = saveUser(2, "B1审批人");
        User b2Operator = saveUser(3, "B2审批人");
        User c1Operator = saveUser(4, "C1审批人");
        User c2Operator = saveUser(5, "C2审批人");
        User d1Operator = saveUser(6, "D1审批人");
        User d2Operator = saveUser(7, "D2审批人");
        User eOperator = saveUser(8, "E审批人");

        FlowForm form = FlowFormBuilder.builder()
                .name("三路并行审批表单")
                .code("three-branch-parallel-form")
                .addField("申请内容", "content", DataType.STRING)
                .build();

        StartNode aNode = StartNode.builder()
                .name("A")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(formPermission(PermissionType.WRITE))
                        .build())
                .build();
        ApprovalNode b1Node = approvalNode("B1", b1Operator);
        ApprovalNode b2Node = approvalNode("B2", b2Operator);
        ApprovalNode c1Node = approvalNode("C1", c1Operator);
        ApprovalNode c2Node = approvalNode("C2", c2Operator);
        ApprovalNode d1Node = approvalNode("D1", d1Operator);
        ApprovalNode d2Node = approvalNode("D2", d2Operator);
        ApprovalNode eNode = approvalNode("E", eOperator);

        ParallelNode parallelNode = ParallelNode.builder()
                .name("B/C/D并行审批")
                .blocks(
                        ParallelBranchNode.builder().name("B分支").order(1).blocks(b1Node, b2Node).build(),
                        ParallelBranchNode.builder().name("C分支").order(2).blocks(c1Node, c2Node).build(),
                        ParallelBranchNode.builder().name("D分支").order(3).blocks(d1Node, d2Node).build()
                )
                .build();
        EndNode fNode = EndNode.builder().name("F").build();

        Workflow workflow = WorkflowBuilder.builder()
                .title("三路并行汇聚流程")
                .code("three-branch-parallel-join")
                .createdOperator(initiator)
                .form(form)
                .addNode(aNode)
                .addNode(parallelNode)
                .addNode(eNode)
                .addNode(fNode)
                .build();
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> formData = Map.of("content", "并行审批测试");
        createProcess(workflow, aNode, initiator, formData);

        FlowRecord aTodo = assertSingleTodo(initiator, aNode);
        String processId = aTodo.getProcessId();
        approve(aTodo, aNode.actionManager().getActions().get(0), initiator, formData);

        // A 提交后，三条分支的第一级必须同时产生待办，第二级和汇聚节点不能提前产生。
        FlowRecord b1Todo = assertSingleTodo(b1Operator, b1Node);
        assertSingleTodo(c1Operator, c1Node);
        assertSingleTodo(d1Operator, d1Node);
        assertNoTodo(b2Operator);
        assertNoTodo(c2Operator);
        assertNoTodo(d2Operator);
        assertNoTodo(eOperator);
        assertNoRecordAtNode(processId, eNode);
        List<FlowRecord> recordsAtB1 = factory.flowRecordRepository.findProcessRecords(processId);
        List<ProcessNode> processNodesAtB1 = processNodes(b1Todo, b1Operator, formData);
        assertEquals(4, recordsAtB1.size(), "B1 阶段底层应只有 A、B1、C1、D1 四条记录");
        assertProcessNodeStatesMatchRecords(recordsAtB1, processNodesAtB1);

        approve(assertSingleTodo(b1Operator, b1Node), passAction(b1Node), b1Operator, formData);
        assertSingleTodo(b2Operator, b2Node);
        assertNoTodo(eOperator);

        approve(assertSingleTodo(b2Operator, b2Node), passAction(b2Node), b2Operator, formData);
        assertNoTodo(eOperator);
        assertNoRecordAtNode(processId, eNode);

        approve(assertSingleTodo(c1Operator, c1Node), passAction(c1Node), c1Operator, formData);
        FlowRecord c2Todo = assertSingleTodo(c2Operator, c2Node);
        assertNoTodo(eOperator);
        List<FlowRecord> recordsAtC2 = factory.flowRecordRepository.findProcessRecords(processId);
        List<ProcessNode> processNodesAtC2 = processNodes(c2Todo, c2Operator, formData);
        assertEquals(6, recordsAtC2.size(), "C2 阶段底层应有 A、B1、B2、C1、C2、D1 六条记录");
        assertProcessNodeStatesMatchRecords(recordsAtC2, processNodesAtC2);

        approve(assertSingleTodo(c2Operator, c2Node), passAction(c2Node), c2Operator, formData);
        assertNoTodo(eOperator);
        assertNoRecordAtNode(processId, eNode);

        approve(assertSingleTodo(d1Operator, d1Node), passAction(d1Node), d1Operator, formData);
        assertSingleTodo(d2Operator, d2Node);
        assertNoTodo(eOperator);

        // 最后一条分支完成后，E 应且只应产生一个待办。
        approve(assertSingleTodo(d2Operator, d2Node), passAction(d2Node), d2Operator, formData);
        FlowRecord eTodo = assertSingleTodo(eOperator, eNode);
        assertEquals(1, recordsAtNode(processId, eNode).size());

        approve(eTodo, passAction(eNode), eOperator, formData);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(processId);
        assertEquals(8, records.size(), "应记录 A、B1、B2、C1、C2、D1、D2、E 共八个业务节点");
        assertEquals(0, records.stream().filter(FlowRecord::isTodo).count(), "E 审批后不应再有待办");
        assertTrue(records.stream().allMatch(FlowRecord::isFinish), "到达 F 后所有流程记录都应标记为完成");
        assertEquals(1, recordsAtNode(processId, eNode).size(), "并行汇聚不得重复创建 E 节点记录");

        // ProcessNodes 是逻辑节点视图，E 虽可从每条并行分支预览到，但只能展示一次。
        // 回归验证：旧实现会在 B1 阶段展示 3 个 E、在 C2 阶段展示 2 个 E。
        assertAll("并行汇聚场景的 ProcessNodes 不应重复展示 E",
                () -> assertEquals(9, processNodesAtB1.size(),
                        "B1 阶段应展示 A、B1、B2、C1、C2、D1、D2、E、F 共九个逻辑节点"),
                () -> assertEquals(1, processNodeCount(processNodesAtB1, eNode),
                        "B1 阶段 E 只能展示一次"),
                () -> assertEquals(List.of("A", "B1", "B2", "C1", "C2", "D1", "D2", "E", "F"),
                        processNodeNames(processNodesAtB1), "B1 阶段应按并行块深度优先展开"),
                () -> assertEquals(9, processNodesAtC2.size(),
                        "C2 阶段仍应展示九个逻辑节点"),
                () -> assertEquals(1, processNodeCount(processNodesAtC2, eNode),
                        "C2 阶段 E 只能展示一次"),
                () -> assertEquals(List.of("A", "B1", "B2", "C1", "C2", "D1", "D2", "E", "F"),
                        processNodeNames(processNodesAtC2), "C2 阶段应保持相同的树形展开顺序"));
    }

    private User saveUser(long userId, String name) {
        User user = new User(userId, name);
        factory.userGateway.save(user);
        return user;
    }

    private ApprovalNode approvalNode(String name, User operator) {
        String script = "def run(request){return [" + operator.getUserId() + "]}";
        return ApprovalNode.builder()
                .name(name)
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(formPermission(PermissionType.READ))
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(script).getKey()))
                        .build())
                .build();
    }

    private FormFieldPermissionStrategy formPermission(PermissionType permissionType) {
        return new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                .addPermission("three-branch-parallel-form", "content", permissionType)
                .build());
    }

    private void createProcess(Workflow workflow, StartNode startNode, User initiator,
                               Map<String, Object> formData) {
        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode(workflow.getCode());
        request.setFormData(formData);
        request.setActionId(startNode.actionManager().getActions().get(0).id());
        request.setOperatorId(initiator.getUserId());
        factory.flowService.create(request);
    }

    private IFlowAction passAction(ApprovalNode node) {
        return node.actionManager().getActions().get(0);
    }

    private void approve(FlowRecord record, IFlowAction action, User operator,
                         Map<String, Object> formData) {
        FlowActionRequest request = new FlowActionRequest();
        request.setRecordId(record.getId());
        request.setFormData(formData);
        request.setAdvice(new FlowAdviceBody(action.id(), "同意", operator.getUserId()));
        factory.flowService.action(request);
    }

    private FlowRecord assertSingleTodo(User operator, IFlowNode node) {
        List<FlowRecord> todos = factory.flowRecordRepository.findTodoByOperator(operator.getUserId());
        assertEquals(1, todos.size(), operator.getName() + " 应有且只有一个待办");
        assertEquals(node.getId(), todos.get(0).getNodeId());
        return todos.get(0);
    }

    private void assertNoTodo(User operator) {
        assertEquals(0, factory.flowRecordRepository.findTodoByOperator(operator.getUserId()).size(),
                operator.getName() + " 不应提前收到待办");
    }

    private List<FlowRecord> recordsAtNode(String processId, ApprovalNode node) {
        return factory.flowRecordRepository.findProcessRecords(processId).stream()
                .filter(record -> node.getId().equals(record.getNodeId()))
                .toList();
    }

    private void assertNoRecordAtNode(String processId, ApprovalNode node) {
        assertEquals(0, recordsAtNode(processId, node).size(), node.getName() + " 不应提前创建流程记录");
    }

    private List<ProcessNode> processNodes(FlowRecord currentRecord, User viewer,
                                           Map<String, Object> formData) {
        return factory.flowService.processNodes(
                new FlowProcessNodeRequest(currentRecord.getId(), viewer.getUserId(), formData));
    }

    private void assertProcessNodeStatesMatchRecords(List<FlowRecord> records, List<ProcessNode> nodes) {
        for (FlowRecord record : records) {
            List<ProcessNode> matchedNodes = nodes.stream()
                    .filter(node -> record.getNodeId().equals(node.getNodeId()))
                    .toList();
            assertEquals(1, matchedNodes.size(), record.getNodeName() + " 应对应一个 ProcessNode");
            ProcessNode.ApproveState expectedState = record.isDone()
                    ? ProcessNode.ApproveState.PASS
                    : ProcessNode.ApproveState.PROCESSING;
            assertEquals(expectedState, matchedNodes.get(0).getApproveState(),
                    record.getNodeName() + " 的流程节点状态应与底层记录一致");
        }
    }

    private long processNodeCount(List<ProcessNode> nodes, IFlowNode flowNode) {
        return nodes.stream().filter(node -> flowNode.getId().equals(node.getNodeId())).count();
    }

    private List<String> processNodeNames(List<ProcessNode> nodes) {
        return nodes.stream().map(ProcessNode::getNodeName).toList();
    }
}

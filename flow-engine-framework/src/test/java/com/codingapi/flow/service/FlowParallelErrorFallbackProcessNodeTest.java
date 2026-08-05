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
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.HandleNode;
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
import com.codingapi.flow.strategy.node.ErrorTriggerStrategy;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.INodeStrategy;
import com.codingapi.flow.strategy.node.MultiOperatorAuditStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 并行分支人员加载异常后回退开始节点的 ProcessNodes 回归测试。
 *
 * <p>流程结构：A -> parallel(B1 -> B2, C1 -> C2, D1 -> D2) -> E -> F。</p>
 * <p>B1 正常生成待办；C1、D1 无法加载办理人，通过 ErrorTriggerStrategy 回退 A。</p>
 * <p>预期底层记录：A(已办)、A(待办)×2、B1(待办)，与项目现场 3321～3324 的结构一致。</p>
 */
class FlowParallelErrorFallbackProcessNodeTest {

    private static final String WORK_CODE = "parallel-error-fallback-process-node";
    private static final String FORM_CODE = "parallel-error-fallback-form";

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    /**
     * 测试目标：复现 B1 正常进入、C1/D1 异常回退 A 时的四条底层记录，
     * 并验证从 B1 查询 ProcessNodes 后仍按 B、C、D 块展示且 F 位于最后。
     *
     * <p>前置条件：B1 正常匹配人员；C1、D1 的人员脚本返回不存在的用户，异常脚本返回开始节点 ID。</p>
     * <p>执行步骤：创建流程并提交 A，再使用 B1 的真实待办记录查询 ProcessNodes。</p>
     * <p>期望断言：四条记录的 fromId/并行信息正确；当前 B1 不重复；展示顺序为
     * A、A、B1、B2、C1、C2、D1、D2、E、F。</p>
     */
    @Test
    void shouldKeepEndLastWhenTwoParallelBranchesFallbackToStart() {
        User initiator = saveUser(1, "发起人");
        User b1Operator = saveUser(2, "B1办理人");
        User b2Operator = saveUser(3, "B2办理人");
        User c2Operator = saveUser(4, "C2办理人");
        User d2Operator = saveUser(5, "D2办理人");
        User eOperator = saveUser(6, "E办理人");

        FlowForm form = FlowFormBuilder.builder()
                .name("并行异常回退表单")
                .code(FORM_CODE)
                .addField("申请内容", "content", DataType.STRING)
                .build();

        StartNode aNode = StartNode.builder()
                .name("A")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(formPermission(PermissionType.WRITE))
                        .build())
                .build();
        HandleNode b1Node = handleNode("B1", b1Operator);
        HandleNode b2Node = handleNode("B2", b2Operator);
        HandleNode c1Node = fallbackHandleNode("C1", aNode);
        HandleNode c2Node = handleNode("C2", c2Operator);
        HandleNode d1Node = fallbackHandleNode("D1", aNode);
        HandleNode d2Node = handleNode("D2", d2Operator);
        // 模拟项目配置：业务节点 E 带有较大 order，而 End 保持默认 order=0。
        // order 不应改变顶层线性拓扑 E -> F 的展示顺序。
        HandleNode eNode = HandleNode.builder()
                .name("E")
                .order(10)
                .strategies(handleStrategies(eOperator))
                .build();
        EndNode fNode = EndNode.builder().name("F").build();

        ParallelNode parallelNode = ParallelNode.builder()
                .name("B/C/D并行")
                .blocks(
                        ParallelBranchNode.builder().name("C分支").order(2).blocks(c1Node, c2Node).build(),
                        ParallelBranchNode.builder().name("D分支").order(3).blocks(d1Node, d2Node).build(),
                        ParallelBranchNode.builder().name("B分支").order(1).blocks(b1Node, b2Node).build())
                .build();

        Workflow workflow = WorkflowBuilder.builder()
                .title("并行异常回退节点记录测试")
                .code(WORK_CODE)
                .createdOperator(initiator)
                .form(form)
                .addNode(aNode)
                .addNode(parallelNode)
                .addNode(eNode)
                .addNode(fNode)
                .build();
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("content", "模拟项目异常回退数据");
        long aRecordId = create(workflow, aNode, initiator, data);
        FlowRecord aRecord = factory.flowRecordRepository.get(aRecordId);
        approve(aRecord, passAction(aNode), initiator, data);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(aRecord.getProcessId()).stream()
                .sorted(Comparator.comparingLong(FlowRecord::getId))
                .toList();
        List<FlowRecord> fallbackStartRecords = records.stream()
                .filter(record -> record.getNodeId().equals(aNode.getId()) && record.isTodo())
                .toList();
        FlowRecord b1Todo = records.stream()
                .filter(record -> record.getNodeId().equals(b1Node.getId()) && record.isTodo())
                .findFirst()
                .orElseThrow();

        assertAll("四条底层记录应与项目现场结构一致",
                () -> assertEquals(4, records.size()),
                () -> assertTrue(records.get(0).isDone()),
                () -> assertEquals(aNode.getId(), records.get(0).getNodeId()),
                () -> assertEquals(List.of(aNode.getId(), aNode.getId(), aNode.getId(), b1Node.getId()),
                        records.stream().map(FlowRecord::getNodeId).toList(),
                        "记录创建顺序应对应现场的 3321=A、3322=A、3323=A、3324=B1"),
                () -> assertEquals(2, fallbackStartRecords.size(), "C1、D1 应各回退生成一条 A 待办"),
                () -> assertTrue(fallbackStartRecords.stream()
                        .allMatch(record -> record.getFromId() == aRecord.getId())),
                () -> assertTrue(fallbackStartRecords.stream()
                                .allMatch(record -> record.getActionId() == null
                                        && record.getActionType() == null
                                        && record.getActionName() == null
                                        && record.getAdvice() == null
                                        && record.getSignKey() == null),
                        "未审批的异常回退 A 记录不应继承上游审批信息"),
                () -> assertEquals(aRecord.getId(), b1Todo.getFromId()),
                () -> assertEquals(1, records.subList(1, 4).stream()
                        .map(FlowRecord::getParallelId).distinct().count()),
                () -> assertTrue(records.subList(1, 4).stream()
                        .allMatch(record -> record.getParallelBranchTotal() == 3)));

        List<ProcessNode> processNodes = factory.flowService.processNodes(
                new FlowProcessNodeRequest(b1Todo.getId(), b1Operator.getUserId(), data));
        List<String> names = processNodes.stream().map(ProcessNode::getNodeName).toList();
        ProcessNode fallbackStartNode = processNodes.stream()
                .filter(node -> node.getNodeId().equals(aNode.getId())
                        && node.getApproveState() == ProcessNode.ApproveState.PROCESSING)
                .findFirst()
                .orElseThrow();

        assertAll("异常回退场景的 ProcessNodes 展示",
                () -> assertEquals(List.of("A", "A", "B1", "B2", "C1", "C2", "D1", "D2", "E", "F"),
                        names, "应先按 B/C/D 块展示，再展示公共 E/F"),
                () -> assertEquals(1, names.stream().filter("B1"::equals).count(),
                        "已有真实待办的 B1 不应再显示一条预览记录"),
                () -> assertEquals("F", names.get(names.size() - 1), "结束节点必须位于最后"),
                () -> assertFalse(processNodes.get(processNodes.size() - 1).isHistory()),
                () -> assertEquals(List.of(ProcessNode.ApproveState.PASS, ProcessNode.ApproveState.PROCESSING),
                        processNodes.stream().filter(node -> node.getNodeId().equals(aNode.getId()))
                                .map(ProcessNode::getApproveState).toList()),
                () -> assertEquals(2, fallbackStartNode.getOperators().size()),
                () -> assertTrue(fallbackStartNode.getOperators().stream()
                                .allMatch(operator -> operator.getActionType() == null
                                        && operator.getActionName() == null
                                        && operator.getAdvice() == null
                                        && operator.getSignKey() == null
                                        && operator.getApproveTime() == 0),
                        "PROCESSING 节点内的未审批记录不应显示上游审批信息"));
    }

    private HandleNode fallbackHandleNode(String name, StartNode fallbackNode) {
        String operatorScript = FlowGroovyScriptFactory
                .createOperatorLoadScript("def run(request){return [-1]}").getKey();
        String errorScript = FlowGroovyScriptFactory
                .createErrorTriggerScript("def run(request){return '" + fallbackNode.getId() + "'}").getKey();
        return HandleNode.builder()
                .name(name)
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(formPermission(PermissionType.READ))
                        .addStrategy(new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.MERGE, 1))
                        .addStrategy(new OperatorLoadStrategy(operatorScript))
                        .addStrategy(new ErrorTriggerStrategy(errorScript))
                        .build())
                .build();
    }

    private HandleNode handleNode(String name, User operator) {
        return HandleNode.builder()
                .name(name)
                .strategies(handleStrategies(operator))
                .build();
    }

    private List<INodeStrategy> handleStrategies(User operator) {
        String operatorScript = FlowGroovyScriptFactory.createOperatorLoadScript(
                "def run(request){return [" + operator.getUserId() + "]}").getKey();
        return NodeStrategyBuilder.builder()
                .addStrategy(formPermission(PermissionType.READ))
                .addStrategy(new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.MERGE, 1))
                .addStrategy(new OperatorLoadStrategy(operatorScript))
                .build();
    }

    private FormFieldPermissionStrategy formPermission(PermissionType permissionType) {
        return new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                .addPermission(FORM_CODE, "content", permissionType)
                .build());
    }

    private User saveUser(long id, String name) {
        User user = new User(id, name);
        factory.userGateway.save(user);
        return user;
    }

    private long create(Workflow workflow, StartNode startNode, User initiator, Map<String, Object> data) {
        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode(workflow.getCode());
        request.setFormData(data);
        request.setActionId(passAction(startNode).id());
        request.setOperatorId(initiator.getUserId());
        return factory.flowService.create(request);
    }

    private void approve(FlowRecord record, IFlowAction action, User operator, Map<String, Object> data) {
        FlowActionRequest request = new FlowActionRequest();
        request.setRecordId(record.getId());
        request.setFormData(data);
        request.setAdvice(new FlowAdviceBody(action.id(), "通过", operator.getUserId()));
        factory.flowService.action(request);
    }

    private IFlowAction passAction(IFlowNode node) {
        return node.actionManager().getActionByType(ActionType.PASS.name());
    }
}

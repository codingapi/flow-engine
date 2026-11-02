package com.codingapi.flow.service;

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
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowDetailRequest;
import com.codingapi.flow.pojo.response.FlowContent;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.record.FlowTodoRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.RecordMergeStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * issue #240：合并审批过程中，审批人只处理合并组中的部分记录时，
 * 剩余记录与后续节点记录出现混乱。
 *
 * <p>流程 A-B-C-D，B、C 均为审批节点且审批人都是 b1，两个节点都开启记录合并。
 * a1、a2、a3、a4 四条流程提交到 B 节点，b1 在合并待办中先审批 a1、a2。</p>
 *
 * <p>期望：a1、a2 的流程记录流转到 C 节点并合并为 C 节点的一条合并待办，
 * 而 a3、a4 的合并待办仍然留在 B 节点。</p>
 */
public class FlowIssue240MergePartialApproveTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    @Test
    void partialApproveOfMergedTodo_shouldKeepRemainingRecordsAtBAndMergeApprovedAtC() {
        // given A-B-C-D，B、C 审批人都是 b1，均开启审批人合并
        User a1 = new User(11, "a1");
        User a2 = new User(12, "a2");
        User a3 = new User(13, "a3");
        User a4 = new User(14, "a4");
        User b1 = new User(2, "b1");
        factory.userGateway.save(a1);
        factory.userGateway.save(a2);
        factory.userGateway.save(a3);
        factory.userGateway.save(a4);
        factory.userGateway.save(b1);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        Workflow workflow = buildWorkflow(a1, "issue240");
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne");

        // 1. a1、a2、a3、a4 各自发起并提交到 B 节点（a1 最后提交，
//    合并待办行的代表记录即为 a1 的记录，与问题现场一致）
        createAndSubmit(a2, workflow, data);
        createAndSubmit(a3, workflow, data);
        createAndSubmit(a4, workflow, data);
        createAndSubmit(a1, workflow, data);

        // B 节点 4 条记录按审批人 b1 合并为 1 条待办
        List<FlowRecord> bTodos = factory.flowRecordRepository.findTodoByOperator(b1.getUserId());
        assertEquals(4, bTodos.size(), "B 节点应有 4 条待办记录");
        List<FlowTodoRecord> bTodoGroups = factory.flowTodoRecordRepository.findByOperatorId(b1.getUserId());
        assertEquals(1, bTodoGroups.size(), "B 节点应合并为 1 条待办组");
        assertEquals(4, bTodoGroups.get(0).getMargeCount(), "B 节点合并组应有 4 条记录");

        // 2. b1 从待办列表打开合并待办（代表记录为 a1 的 B 记录），选择 a1、a2 审批
        long representativeRecordId = bTodoGroups.get(0).getRecordId();
        assertEquals(a1.getUserId(),
                factory.flowRecordRepository.get(representativeRecordId).getCreateOperatorId(),
                "合并待办行的代表记录应为最后提交的 a1");
        FlowContent detail = factory.flowService.detail(
                new FlowDetailRequest(representativeRecordId, b1.getUserId()));
        assertEquals(4, detail.getTodos().size(), "合并详情应展示 4 条记录");

        List<FlowContent.Body> selected = detail.getTodos().stream()
                .filter(todo -> isCreator(todo, a1) || isCreator(todo, a2))
                .toList();
        assertEquals(2, selected.size(), "应能选中 a1、a2 的记录");

        for (FlowContent.Body todo : selected) {
            approve(b1, todo.getRecordId(), workflow, data);
        }

        // then a3、a4 的合并待办仍保留在 B 节点
        List<FlowTodoRecord> todosAfterApprove = factory.flowTodoRecordRepository.findByOperatorId(b1.getUserId());
        FlowTodoRecord bGroup = findTodoGroup(todosAfterApprove, bNodeId(workflow));
        assertNotNull(bGroup, "B 节点的合并待办不应消失");
        assertEquals(2, bGroup.getMargeCount(), "B 节点合并组应剩余 a3、a4 两条记录");
        assertEquals(2, countTodoRecords(b1.getUserId(), bNodeId(workflow)),
                "B 节点应剩余 2 条待办记录(4 条记录中 2 条已审批)");

        // a1、a2 应流转到 C 节点并合并为一条 C 节点待办
        FlowTodoRecord cGroup = findTodoGroup(todosAfterApprove, cNodeId(workflow));
        assertNotNull(cGroup, "C 节点应产生合并待办");
        assertEquals(2, cGroup.getMargeCount(), "C 节点合并组应包含 a1、a2 两条记录");
        assertEquals(2, countTodoRecords(b1.getUserId(), cNodeId(workflow)),
                "C 节点应有 2 条待办记录");

        // 待办组总数应为 2（B 组 + C 组），不应出现多余的代表记录
        assertEquals(2, todosAfterApprove.size(), "b1 的待办组应为 B 节点组与 C 节点组共 2 条");

        // B 组的代表记录必须仍是未办理的待办记录（a3 或 a4），
        // 否则待办列表行会指向已完成的 a1 记录，打开后无法处理剩余记录
        FlowRecord bRepresentative = factory.flowRecordRepository.get(bGroup.getRecordId());
        FlowContent bDetail = factory.flowService.detail(
                new FlowDetailRequest(bGroup.getRecordId(), b1.getUserId()));
        assertAll("部分审批后 B 组合并待办的代表记录与详情",
                () -> assertNotNull(bRepresentative, "B 组合并代表记录应存在"),
                () -> assertTrue(bRepresentative.isTodo(),
                        "B 组合并代表记录应仍为待办，实际指向：" + bRepresentative.getId()
                                + "(recordState=" + bRepresentative.getRecordState() + ")"),
                () -> assertEquals(bNodeId(workflow), bRepresentative.getNodeId(),
                        "B 组合并代表记录应属于 B 节点"),
                // 以待办列表行（代表记录）打开详情，仍应展示剩余的 a3、a4 两条合并记录
                () -> assertEquals(2, bDetail.getTodos().size(),
                        "B 组合并详情应仍展示 a3、a4 两条记录"),
                () -> assertTrue(bDetail.isMergeable(), "B 组详情应保持可合并审批状态"),
                () -> assertTrue(bDetail.getActions() != null && !bDetail.getActions().isEmpty(),
                        "B 组详情应仍可执行审批动作"));

        // C 节点合并组中的流程记录应与 a1、a2 对应
        Set<Long> cCreators = factory.flowRecordRepository.findTodoByOperator(b1.getUserId()).stream()
                .filter(r -> r.getNodeId().equals(cNodeId(workflow)))
                .map(FlowRecord::getCreateOperatorId)
                .collect(Collectors.toSet());
        assertEquals(Set.of(a1.getUserId(), a2.getUserId()), cCreators,
                "C 节点待办应属于 a1、a2");
    }

    // ─── 辅助方法 ───

    private boolean isCreator(FlowContent.Body content, User user) {
        return content.getCreatedOperator() != null
                && content.getCreatedOperator().getId() == user.getUserId();
    }

    private FlowTodoRecord findTodoGroup(List<FlowTodoRecord> todos, String nodeId) {
        return todos.stream()
                .filter(todo -> nodeId.equals(todo.getNodeId()))
                .findFirst().orElse(null);
    }

    private long countTodoRecords(long operatorId, String nodeId) {
        return factory.flowRecordRepository.findTodoByOperator(operatorId).stream()
                .filter(record -> nodeId.equals(record.getNodeId()))
                .count();
    }

    private String bNodeId(Workflow workflow) {
        return workflow.getNodes().stream()
                .filter(node -> node.getName().equals("B审批"))
                .findFirst().orElseThrow().getId();
    }

    private String cNodeId(Workflow workflow) {
        return workflow.getNodes().stream()
                .filter(node -> node.getName().equals("C审批"))
                .findFirst().orElseThrow().getId();
    }

    private Workflow buildWorkflow(User createdBy, String code) {
        FlowForm form = FlowFormBuilder.builder()
                .name("测试流程")
                .code(code)
                .addField("name", "name", DataType.STRING)
                .build();

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission(code, "name", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();

        ApprovalNode bNode = ApprovalNode.builder()
                .name("B审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission(code, "name", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .addStrategy(new RecordMergeStrategy(true))
                        .build())
                .build();

        ApprovalNode cNode = ApprovalNode.builder()
                .name("C审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission(code, "name", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .addStrategy(new RecordMergeStrategy(true))
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();

        return WorkflowBuilder.builder()
                .title("测试流程")
                .code(code)
                .createdOperator(createdBy)
                .form(form)
                .addNode(startNode)
                .addNode(bNode)
                .addNode(cNode)
                .addNode(endNode)
                .build();
    }

    private String createAndSubmit(User user, Workflow workflow, Map<String, Object> data) {
        StartNode startNode = (StartNode) workflow.getStartNode();
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(user.getUserId());
        long recordId = factory.flowService.create(createRequest);

        FlowRecord record = factory.flowRecordRepository.get(recordId);
        String processId = record.getProcessId();

        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(data);
        actionRequest.setRecordId(recordId);
        actionRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(actionRequest);
        return processId;
    }

    private void approve(User approver, long recordId, Workflow workflow, Map<String, Object> data) {
        FlowRecord record = factory.flowRecordRepository.get(recordId);
        List<IFlowAction> actions = workflow.getFlowNode(record.getNodeId()).actionManager().getActions();
        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(data);
        actionRequest.setRecordId(recordId);
        actionRequest.setAdvice(new FlowAdviceBody(actions.get(0).id(), "同意", approver.getUserId()));
        factory.flowService.action(actionRequest);
    }
}
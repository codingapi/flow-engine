package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.action.actions.PassAction;
import com.codingapi.flow.action.actions.RejectAction;
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
import com.codingapi.flow.pojo.request.FlowProcessNodeRequest;
import com.codingapi.flow.pojo.response.FlowContent;
import com.codingapi.flow.pojo.response.ProcessNode;
import com.codingapi.flow.record.FlowRecord;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * issue #203 合并审批场景：C 节点合并审批（提交人合并），其中一条被拒绝退回后重新提交，
 * 两条流程最终都结束后，未退回的那条流程记录不应出现拒绝过程的记录。
 */
public class FlowIssue203MergeRejectTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    @Test
    void rejectOneOfMergedThenResubmit_shouldNotPolluteTheOther() {
        // given 流程 A-B-C-D，B 审批人 b1，C 审批人 c1 且开启提交人合并，
        // C 节点拒绝动作回到发起节点
        User a = new User(1, "a");
        User b1 = new User(2, "b1");
        User c1 = new User(3, "c1");
        factory.userGateway.save(a);
        factory.userGateway.save(b1);
        factory.userGateway.save(c1);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        Workflow workflow = buildWorkflow(a, "issue203",
                "def run(request){return [2]}", "def run(request){return [3]}");
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne");

        // 1. A 发起两条流程 P1、P2
        String p1ProcessId = createAndSubmit(a, workflow, data);
        String p2ProcessId = createAndSubmit(a, workflow, data);

        // 2. b1 审批两条通过 → 到达 C 节点
        approveAllTodo(b1, workflow, data);

        // C 节点两条记录合并为一条待办
        assertEquals(2, factory.flowRecordRepository.findTodoByOperator(c1.getUserId()).size());
        assertEquals(1, factory.flowTodoRecordRepository.findByOperatorId(c1.getUserId()).size());

        // 3. c1 拒绝第一条流程（P1）→ 回到发起节点
        List<FlowRecord> cTodoRecords = factory.flowRecordRepository.findTodoByOperator(c1.getUserId());
        FlowRecord p1CRecord = cTodoRecords.stream()
                .filter(record -> record.getProcessId().equals(p1ProcessId))
                .findFirst().orElseThrow();
        reject(c1, p1CRecord, workflow, data);

        // 拒绝后：P1 回到 A，P2 仍在 C 节点等待
        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(a.getUserId()).size());
        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(c1.getUserId()).size());

        // 4. A 重新提交 P1 → b1 审批 → 到达 C 节点，与 P2 重新合并
        approveAllTodo(a, workflow, data);
        approveAllTodo(b1, workflow, data);
        assertEquals(2, factory.flowRecordRepository.findTodoByOperator(c1.getUserId()).size());
        assertEquals(1, factory.flowTodoRecordRepository.findByOperatorId(c1.getUserId()).size());

        // 5. c1 将两条记录全部通过 → 流程结束
        approveAllTodo(c1, workflow, data);

        // then P1（被拒绝过）与 P2（未拒绝）的流程记录应各自独立
        List<FlowRecord> p1Records = factory.flowRecordRepository.findProcessRecords(p1ProcessId);
        List<FlowRecord> p2Records = factory.flowRecordRepository.findProcessRecords(p2ProcessId);

        System.out.println("P1 流程记录：" + p1Records.stream()
                .map(r -> r.getNodeId() + "[" + r.getActionType() + "]").toList());
        System.out.println("P2 流程记录：" + p2Records.stream()
                .map(r -> r.getNodeId() + "[" + r.getActionType() + "]").toList());

        // P1 应包含拒绝过程的记录
        assertTrue(p1Records.stream().anyMatch(record -> "REJECT".equals(record.getActionType())),
                "P1（被退回）应包含拒绝过程记录");
        // P2 不应包含拒绝过程的记录（issue #203 的核心问题）
        assertTrue(p2Records.stream().noneMatch(record -> "REJECT".equals(record.getActionType())),
                "P2（未退回）不应包含拒绝过程记录");

        // 展示层验证：以 P2 的一条记录查看流程详情与节点记录，不应出现拒绝过程
        FlowRecord p2CRecord = p2Records.stream()
                .filter(record -> record.getNodeId().equals(cNodeId(workflow)))
                .findFirst().orElseThrow();

        FlowContent p2Detail = factory.flowService.detail(
                new FlowDetailRequest(p2CRecord.getId(), c1.getUserId()));
        // 详情历史为当前记录之前的记录（不含记录本身），P2 的正常历史为 [A, B]，
        // C 节点出现次数应为 0；若出现（REJECT + PASS）则说明被 P1 的记录污染
        long cNodeHistoryCount = p2Detail.getHistories() == null ? 0
                : p2Detail.getHistories().stream()
                        .filter(h -> h.getNodeId().equals(cNodeId(workflow)))
                        .count();
        System.out.println("P2 detail 历史 C 节点出现次数：" + cNodeHistoryCount);
        assertEquals(0, cNodeHistoryCount, "P2 详情历史不应包含当前 C 节点记录（未被污染）");

        List<ProcessNode> p2Nodes = factory.flowService.processNodes(
                new FlowProcessNodeRequest(p2CRecord.getId(), c1.getUserId(), data));
        boolean nodeHasReject = p2Nodes.stream()
                .anyMatch(n -> n.getOperators() != null && n.getOperators().stream()
                        .anyMatch(op -> "REJECT".equals(op.getActionType())));
        System.out.println("P2 processNodes：" + p2Nodes.stream()
                .map(n -> n.getNodeName() + "[" + n.getApproveState() + "]").toList());
        assertTrue(!nodeHasReject, "P2 节点记录不应包含拒绝过程记录");
    }

    private String cNodeId(Workflow workflow) {
        return workflow.getNodes().stream()
                .filter(node -> node.getName().equals("C审批"))
                .findFirst().orElseThrow().getId();
    }

    // ─── 辅助方法 ───

    private Workflow buildWorkflow(User createdBy, String code, String bScript, String cScript) {
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
                                FlowGroovyScriptFactory.createOperatorLoadScript(bScript).getKey()))
                        .build())
                .build();

        // C 节点：审批人 c1 + 提交人合并 + 拒绝动作回到发起节点
        RejectAction rejectAction = RejectAction.defaultAction();
        rejectAction.setScript(FlowGroovyScriptFactory
                .createActionRejectScript("def run(request){return request.getStartNode().getId()}")
                .getKey());
        ApprovalNode cNode = ApprovalNode.builder()
                .name("C审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission(code, "name", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(cScript).getKey()))
                        .addStrategy(new RecordMergeStrategy(true, RecordMergeStrategy.MergeType.SUBMITTER))
                        .build())
                .actions(List.of(PassAction.defaultAction(), rejectAction))
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

    private void approveAllTodo(User approver, Workflow workflow, Map<String, Object> data) {
        List<FlowRecord> todoRecords = factory.flowRecordRepository.findTodoByOperator(approver.getUserId());
        for (FlowRecord record : todoRecords) {
            approve(approver, record, workflow, data);
        }
    }

    private void approve(User approver, FlowRecord record, Workflow workflow, Map<String, Object> data) {
        List<IFlowAction> actions = workflow.getFlowNode(record.getNodeId()).actionManager().getActions();
        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(data);
        actionRequest.setRecordId(record.getId());
        actionRequest.setAdvice(new FlowAdviceBody(actions.get(0).id(), "同意", approver.getUserId()));
        factory.flowService.action(actionRequest);
    }

    private void reject(User approver, FlowRecord record, Workflow workflow, Map<String, Object> data) {
        List<IFlowAction> actions = workflow.getFlowNode(record.getNodeId()).actionManager().getActions();
        // RejectAction 是第二个动作
        IFlowAction rejectAction = actions.stream()
                .filter(action -> action.type().equals("REJECT"))
                .findFirst().orElseThrow();
        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(data);
        actionRequest.setRecordId(record.getId());
        actionRequest.setAdvice(new FlowAdviceBody(rejectAction.id(), "拒绝", approver.getUserId()));
        factory.flowService.action(actionRequest);
    }
}
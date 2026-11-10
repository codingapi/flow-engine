package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
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
import com.codingapi.flow.node.nodes.ManualBranchNode;
import com.codingapi.flow.node.nodes.ManualNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.response.ActionResponse;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * issue #259 复现测试：人工节点分支下存在审批人设定节点
 *
 * <p>流程设计：A(开始) -> B(人工节点) -> C1(审批人设定) / C2(审批人设定) -> D(结束)。
 *
 * <p>问题：发起人提交开始节点后首先弹出人工节点的分支选择框；选择 C1 所在分支后，
 * 引擎未继续弹出 C1 节点的审批人设定框，流程直接流转到 C1，导致 C1 无审批人而异常。
 *
 * <p>期望：选择分支后紧跟着提示为 C1 设定审批人；只有选中的分支才会被提示，
 * 未选中分支（C2）不应出现在提示中。
 */
class FlowIssue259ManualBranchApproverSelectTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    private ApprovalNode c1Node;
    private ApprovalNode c2Node;
    private ManualBranchNode c1BranchNode;

    /**
     * 选择 C1 分支后应提示为 C1 设定审批人，指定后流程流转到 C1。
     *
     * <p>前置条件：流程为 A(开始) -> B(人工) -> C1/C2(审批人设定) -> D(结束)。
     * <p>执行步骤：发起流程 -> 提交开始节点 -> 选择 C1 分支 -> 为 C1 指定审批人。
     * <p>期望：选择分支的响应为 OPERATOR_SELECT 且只包含 C1；指定审批人后 C1 收到待办。
     */
    @Test
    void shouldPromptApproverSelectAfterSelectingManualBranch() {
        // given
        User user = new User(1, "user");
        User c1User = new User(2, "c1User");
        User c2User = new User(3, "c2User");
        factory.userGateway.save(user);
        factory.userGateway.save(c1User);
        factory.userGateway.save(c2User);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        Workflow workflow = buildWorkflow(user);
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "reason", "leave");

        List<IFlowAction> startActions = workflow.getStartNode().actionManager().getActions();
        createFlow(workflow, user, data, startActions);
        FlowRecord startRecord = todoOf(user, "发起人应收到开始节点待办");

        // when: 第一次提交开始节点，应提示选择人工节点分支
        ActionResponse manualResponse = submitStart(startActions, startRecord, user, data, null, null);

        // then
        assertNotNull(manualResponse, "开始节点提交后应提示选择人工节点分支");
        assertEquals(ActionResponse.ResponseType.MANUAL_NODE_SELECT, manualResponse.getResponseType());
        assertEquals(2, manualResponse.getOptions().size());

        // when: 选择 C1 所在分支后再次提交
        ActionResponse operatorResponse = submitStart(
                startActions, startRecord, user, data, c1BranchNode.getId(), null);

        // then: 应紧跟着提示为 C1 设定审批人，且只提示选中分支内的 C1
        assertNotNull(operatorResponse, "选择 C1 分支后应提示为 C1 节点设定审批人");
        assertEquals(ActionResponse.ResponseType.OPERATOR_SELECT, operatorResponse.getResponseType());
        assertEquals(1, operatorResponse.getOptions().size());
        assertEquals(c1Node.getId(), operatorResponse.getOptions().get(0).getId());

        // when: 为 C1 指定审批人后再次提交
        ActionResponse finishResponse = submitStart(
                startActions, startRecord, user, data, c1BranchNode.getId(),
                Map.of(c1Node.getId(), List.of(c1User.getUserId())));

        // then: 流程流转到 C1，未选中分支的 C2 不产生待办
        assertNull(finishResponse);
        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(c1User.getUserId()).size(),
                "为 C1 指定审批人后，流程应流转到 C1 节点");
        assertEquals(0, factory.flowRecordRepository.findTodoByOperator(c2User.getUserId()).size(),
                "未选中分支的 C2 不应产生待办");
    }

    // ---------- 辅助方法 ----------

    private Workflow buildWorkflow(User user) {
        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder().addStrategy(writePermission()).build())
                .build();

        // C1、C2 均为审批人设定（APPROVER_SELECT）节点
        c1Node = approverSelectNode("C1审批人设定");
        c2Node = approverSelectNode("C2审批人设定");

        c1BranchNode = ManualBranchNode.builder()
                .name("C1分支")
                .blocks(c1Node)
                .order(1)
                .build();

        ManualBranchNode c2BranchNode = ManualBranchNode.builder()
                .name("C2分支")
                .blocks(c2Node)
                .order(2)
                .build();

        ManualNode manualNode = ManualNode.builder()
                .name("B人工节点")
                .blocks(c1BranchNode, c2BranchNode)
                .build();

        EndNode endNode = EndNode.builder().build();

        return WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(manualNode)
                .addNode(endNode)
                .build();
    }

    private void createFlow(Workflow workflow, User user, Map<String, Object> data, List<IFlowAction> startActions) {
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(user.getUserId());
        factory.flowService.create(createRequest);
    }

    private ActionResponse submitStart(List<IFlowAction> startActions,
                                       FlowRecord record,
                                       User user,
                                       Map<String, Object> data,
                                       String manualNodeId,
                                       Map<String, List<Long>> operatorSelectMap) {
        FlowActionRequest request = new FlowActionRequest();
        request.setFormData(data);
        request.setRecordId(record.getId());
        FlowAdviceBody advice = new FlowAdviceBody(startActions.get(0).id(), "提交", user.getUserId());
        advice.setManualNodeId(manualNodeId);
        advice.setOperatorSelectMap(operatorSelectMap);
        request.setAdvice(advice);
        return factory.flowService.action(request);
    }

    private FlowRecord todoOf(User target, String message) {
        List<FlowRecord> records = factory.flowRecordRepository.findTodoByOperator(target.getUserId());
        assertEquals(1, records.size(), message);
        return records.get(0);
    }

    private ApprovalNode approverSelectNode(String name) {
        return ApprovalNode.builder()
                .name(name)
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(readPermission())
                        .addStrategy(OperatorLoadStrategy.approverSelectStrategy())
                        .build())
                .build();
    }

    private FormFieldPermissionStrategy writePermission() {
        return new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                .addPermission("leave", "name", PermissionType.WRITE)
                .addPermission("leave", "reason", PermissionType.WRITE)
                .build());
    }

    private FormFieldPermissionStrategy readPermission() {
        return new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                .addPermission("leave", "name", PermissionType.READ)
                .addPermission("leave", "reason", PermissionType.READ)
                .build());
    }
}

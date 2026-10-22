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
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.*;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.response.ActionResponse;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * issue #231 复现测试：并行节点下审批人设定节点被提前提示
 *
 * <p>流程设计：A(开始) -> B(审批) -> C(并行) -> D(结束)
 * <p>C 下两条并行分支：
 * <ul>
 *     <li>分支1：A1(审批) -> A2(抄送，审批人设定)</li>
 *     <li>分支2：B1(审批) -> B2(抄送，审批人设定)</li>
 * </ul>
 *
 * <p>问题：B 节点提交审批时就弹出 A2、B2 的审批人设定提示；A1、B1 审批时又再次提示。
 * <p>期望：仅在 A1 提交时提示设定 A2，在 B1 提交时提示设定 B2；B 节点提交时不提示。
 */
class FlowIssue231ParallelApproverSelectTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    private NotifyNode a2Node;
    private NotifyNode b2Node;

    @Test
    void shouldNotPromptApproverSelectAtParallelControlNode() {
        // given
        User user = new User(1, "user");
        User bUser = new User(2, "bUser");
        User a1User = new User(3, "a1User");
        User b1User = new User(4, "b1User");
        User a2User = new User(5, "a2User");
        User b2User = new User(6, "b2User");
        factory.userGateway.save(user);
        factory.userGateway.save(bUser);
        factory.userGateway.save(a1User);
        factory.userGateway.save(b1User);
        factory.userGateway.save(a2User);
        factory.userGateway.save(b2User);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        Workflow workflow = buildWorkflow(user);
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        // when: 发起流程并提交开始节点，B 收到待办
        createAndSubmitStart(workflow, user, data);
        FlowRecord bTodo = todoOf(bUser, "B 节点应收到待办");

        // when: B 节点提交审批（不带 operatorSelectMap）
        ActionResponse response = submitRecord(workflow, bTodo, bUser, data, null);

        // then: B 的下一节点是并行控制节点 C，其分支首节点 A1/B1 都不需要设定审批人，
        // 不应弹出任何审批人设定提示，流程应直接进入 A1、B1
        assertNull(response, "B 节点提交时不应弹出下游抄送节点的审批人设定提示");
        todoOf(a1User, "A1 节点应收到待办");
        todoOf(b1User, "B1 节点应收到待办");

        // when: A1 提交审批（不带 operatorSelectMap）
        FlowRecord a1Todo = todoOf(a1User, "A1 节点应收到待办");
        ActionResponse a1Response = submitRecord(workflow, a1Todo, a1User, data, null);

        // then: 此时才提示为 A2 设定审批人，且仅包含 A2
        assertNotNull(a1Response, "A1 节点提交时应提示为 A2 设定审批人");
        assertEquals(ActionResponse.ResponseType.OPERATOR_SELECT, a1Response.getResponseType());
        assertEquals(1, a1Response.getOptions().size());
        assertEquals(a2Node.getId(), a1Response.getOptions().get(0).getId());

        // when: A1 再次提交并为 A2 指定审批人
        assertNull(submitRecord(workflow, a1Todo, a1User, data,
                Map.of(a2Node.getId(), List.of(a2User.getUserId()))));

        // then: A2 抄送记录生成
        assertEquals(1, doneOf(a2User).size(), "A2 应收到抄送记录");

        // when: B1 提交审批（不带 operatorSelectMap）
        FlowRecord b1Todo = todoOf(b1User, "B1 节点应收到待办");
        ActionResponse b1Response = submitRecord(workflow, b1Todo, b1User, data, null);

        // then: 此时才提示为 B2 设定审批人，且仅包含 B2
        assertNotNull(b1Response, "B1 节点提交时应提示为 B2 设定审批人");
        assertEquals(ActionResponse.ResponseType.OPERATOR_SELECT, b1Response.getResponseType());
        assertEquals(1, b1Response.getOptions().size());
        assertEquals(b2Node.getId(), b1Response.getOptions().get(0).getId());

        // when: B1 再次提交并为 B2 指定审批人
        assertNull(submitRecord(workflow, b1Todo, b1User, data,
                Map.of(b2Node.getId(), List.of(b2User.getUserId()))));

        // then: B2 抄送记录生成
        assertEquals(1, doneOf(b2User).size(), "B2 应收到抄送记录");
    }

    // ---------- 辅助方法 ----------

    private Workflow buildWorkflow(User user) {
        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder().addStrategy(writePermission()).build())
                .build();

        // B 审批节点：固定审批人 bUser(2)
        ApprovalNode bNode = approvalNode("B审批", "def run(request){return [2]}");

        // 分支1：A1(审批) -> A2(抄送，审批人设定)
        ApprovalNode a1Node = approvalNode("A1审批", "def run(request){return [3]}");
        a2Node = notifyApproverSelectNode("A2抄送");
        ParallelBranchNode branch1 = ParallelBranchNode.builder()
                .name("并行分支1")
                .order(1)
                .blocks(a1Node, a2Node)
                .build();

        // 分支2：B1(审批) -> B2(抄送，审批人设定)
        ApprovalNode b1Node = approvalNode("B1审批", "def run(request){return [4]}");
        b2Node = notifyApproverSelectNode("B2抄送");
        ParallelBranchNode branch2 = ParallelBranchNode.builder()
                .name("并行分支2")
                .order(2)
                .blocks(b1Node, b2Node)
                .build();

        ParallelNode parallelNode = ParallelNode.builder()
                .name("并行控制")
                .blocks(branch1, branch2)
                .build();

        EndNode endNode = EndNode.builder().build();

        return WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bNode)
                .addNode(parallelNode)
                .addNode(endNode)
                .build();
    }

    private void createAndSubmitStart(Workflow workflow, User user, Map<String, Object> data) {
        List<IFlowAction> startActions = workflow.getStartNode().actionManager().getActions();
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(user.getUserId());
        factory.flowService.create(createRequest);

        List<FlowRecord> userRecords = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecords.size(), "发起人应收到开始节点待办");

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecords.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "提交", user.getUserId()));
        factory.flowService.action(userRequest);
    }

    private FlowRecord todoOf(User target, String message) {
        List<FlowRecord> records = factory.flowRecordRepository.findTodoByOperator(target.getUserId());
        assertEquals(1, records.size(), message);
        return records.get(0);
    }

    private List<FlowRecord> doneOf(User target) {
        return factory.flowRecordRepository.findDoneByOperator(target.getUserId());
    }

    private ActionResponse submitRecord(Workflow workflow, FlowRecord record, User operator, Map<String, Object> data,
                                        Map<String, List<Long>> operatorSelectMap) {
        IFlowNode node = workflow.getFlowNode(record.getNodeId());
        List<IFlowAction> actions = node.actionManager().getActions();
        FlowActionRequest request = new FlowActionRequest();
        request.setFormData(data);
        request.setRecordId(record.getId());
        FlowAdviceBody advice = new FlowAdviceBody(actions.get(0).id(), "同意", operator.getUserId());
        advice.setOperatorSelectMap(operatorSelectMap);
        request.setAdvice(advice);
        return factory.flowService.action(request);
    }

    private ApprovalNode approvalNode(String name, String operatorScript) {
        return ApprovalNode.builder()
                .name(name)
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(readPermission())
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(operatorScript).getKey()))
                        .build())
                .build();
    }

    private NotifyNode notifyApproverSelectNode(String name) {
        return NotifyNode.builder()
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
                .addPermission("leave", "days", PermissionType.WRITE)
                .addPermission("leave", "reason", PermissionType.WRITE)
                .build());
    }

    private FormFieldPermissionStrategy readPermission() {
        return new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                .addPermission("leave", "name", PermissionType.READ)
                .addPermission("leave", "days", PermissionType.READ)
                .addPermission("leave", "reason", PermissionType.READ)
                .build());
    }
}
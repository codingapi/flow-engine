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
 * issue #200 复现测试：条件分支下的审批人设定节点
 *
 * <p>流程设计：A(开始) -> B(审批) -> C(条件) -> C1(条件分支, 含 D) -> D(审批人设定) -> E(审批) -> F(结束)
 * else 分支：A -> B -> C -> C2(else) -> E -> F，即 C1 分支下存在一个 D 审批人设定节点。
 *
 * <p>问题：B 节点提交审批时，若下一流程实际走 else 分支（不经过 D），引擎仍弹出 D 节点的审批人设定提示。
 * <p>期望：仅当流程实际经过审批人设定节点 D（命中 C1 分支）时才弹出设定提示；走 else 分支（C2）时不弹。
 */
class FlowIssue200ConditionApproverSelectTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    /** D 审批人设定节点（构建工作流后可供断言引用） */
    private ApprovalNode dNode;

    /**
     * else 分支场景（days=5，命中 else 分支 C2，不经过 D）：
     * B 节点提交审批时不应弹出 D 节点的审批人设定提示，流程直接流转到 E。
     */
    @Test
    void shouldNotPromptApproverSelectWhenElseBranchSkipped() {
        // given
        User user = new User(1, "user");
        User bUser = new User(2, "bUser");
        User dUser = new User(3, "dUser");
        User eUser = new User(4, "eUser");
        factory.userGateway.save(user);
        factory.userGateway.save(bUser);
        factory.userGateway.save(dUser);
        factory.userGateway.save(eUser);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        Workflow workflow = buildWorkflow(user);
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 5, "reason", "leave");

        // when: 发起流程并提交开始节点，B 收到待办
        createAndSubmitStart(workflow, user, data);
        FlowRecord bTodo = todoOf(bUser, "B 节点应收到待办");

        // B 提交审批（不带 operatorSelectMap）
        submitRecord(workflow, bTodo, bUser, data, null);

        // then: 走 else 分支（不经过 D），不应返回审批人设定提示，E 直接收到待办
        List<FlowRecord> eRecords = factory.flowRecordRepository.findTodoByOperator(eUser.getUserId());
        assertEquals(1, eRecords.size(), "走 else 分支时应直接流转到 E 节点");
        assertEquals(0, factory.flowRecordRepository.findTodoByOperator(dUser.getUserId()).size(),
                "不经过 D 节点时，D 不应产生待办");
    }

    /**
     * 命中分支场景（days=1，命中 C1 分支，经过 D）：
     * B 节点提交审批时应弹出 D 节点的审批人设定提示，指定审批人后流程流转到 D。
     */
    @Test
    void shouldPromptApproverSelectWhenConditionHit() {
        // given
        User user = new User(1, "user");
        User bUser = new User(2, "bUser");
        User dUser = new User(3, "dUser");
        User eUser = new User(4, "eUser");
        factory.userGateway.save(user);
        factory.userGateway.save(bUser);
        factory.userGateway.save(dUser);
        factory.userGateway.save(eUser);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        Workflow workflow = buildWorkflow(user);
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        // when: 发起流程并提交开始节点，B 收到待办
        createAndSubmitStart(workflow, user, data);
        FlowRecord bTodo = todoOf(bUser, "B 节点应收到待办");

        // B 第一次提交（不带 operatorSelectMap）：应提示为 D 节点设定审批人
        ActionResponse response = submitRecord(workflow, bTodo, bUser, data, null);

        // then
        assertNotNull(response, "命中 C1 分支时，应返回审批人设定提示");
        assertEquals(ActionResponse.ResponseType.OPERATOR_SELECT, response.getResponseType());
        assertEquals(1, response.getOptions().size());
        assertEquals(dNode.getId(), response.getOptions().get(0).getId(), "提示应为 D 节点");

        // when: B 再次提交并为 D 指定审批人 dUser
        assertNull(submitRecord(workflow, bTodo, bUser, data, Map.of(dNode.getId(), List.of(dUser.getUserId()))));

        // then: D 收到待办，流程不直接到 E
        List<FlowRecord> dRecords = factory.flowRecordRepository.findTodoByOperator(dUser.getUserId());
        assertEquals(1, dRecords.size(), "为 D 指定审批人后，流程应流转到 D 节点");
        assertEquals(0, factory.flowRecordRepository.findTodoByOperator(eUser.getUserId()).size());

        // when: dUser 审批通过，E 收到待办
        submitRecord(workflow, dRecords.get(0), dUser, data, null);
        List<FlowRecord> eRecords = factory.flowRecordRepository.findTodoByOperator(eUser.getUserId());
        assertEquals(1, eRecords.size(), "D 审批通过后应流转到 E 节点");

        // when: eUser 审批通过，流程结束
        submitRecord(workflow, eRecords.get(0), eUser, data, null);
        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(eRecords.get(0).getProcessId());
        // 开始、B、D、E 共 4 条业务记录（结束节点虚拟记录不持久化）
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).count(), "流程应正常结束");
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

        // D 审批人设定节点（APPROVER_SELECT）
        dNode = approverSelectNode("D审批人设定");

        // E 审批节点：固定审批人 eUser(4)
        ApprovalNode eNode = approvalNode("E审批", "def run(request){return [4]}");

        // C1 条件分支：days <= 3 时命中，链路为 D -> E
        ConditionBranchNode c1 = ConditionBranchNode.builder()
                .name("条件分支")
                .conditionScript(FlowGroovyScriptFactory.createConditionScript(
                        "def run(request){return request.getFormData('days') <= 3}").getKey())
                .order(1)
                .blocks(dNode, eNode)
                .build();
        // C2 else 分支：不经过 D，链路为 E
        ConditionElseBranchNode c2 = ConditionElseBranchNode.builder()
                .name("else分支")
                .order(2)
                .blocks(eNode)
                .build();
        ConditionNode conditionNode = ConditionNode.builder()
                .name("条件控制")
                .blocks(c1, c2)
                .build();

        EndNode endNode = EndNode.builder().build();

        return WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bNode)
                .addNode(conditionNode)
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
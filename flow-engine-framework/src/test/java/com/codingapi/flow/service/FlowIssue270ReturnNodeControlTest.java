package com.codingapi.flow.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.PassAction;
import com.codingapi.flow.action.actions.ReturnAction;
import com.codingapi.flow.builder.ActionBuilder;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.exception.FlowException;
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
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowDetailRequest;
import com.codingapi.flow.pojo.response.FlowContent;
import com.codingapi.flow.pojo.response.NodeOption;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * issue #270 退回节点的操作控制。
 *
 * <p>流程设计：A(开始) -> B(审批) -> C(审批) -> D(结束)，B、C 均开启退回动作。
 *
 * <ul>
 *     <li>可退回节点只能来自当前记录真实执行过的历史节点，且按“最近优先”排序；</li>
 *     <li>未执行过的分支节点不能作为退回目标（即使它不是当前节点的下游）；</li>
 *     <li>开始节点没有可退回节点，不对外展示退回动作。</li>
 * </ul>
 */
class FlowIssue270ReturnNodeControlTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    /**
     * 线性流程：B 节点待办只能退回 A，C 节点待办按“最近优先”返回 B、A。
     */
    @Test
    void detailShouldProvideHistoryBackNodesNearestFirst() {
        // given：A -> B(b1、b2 或签) -> C(c1) -> D
        User a1 = new User(1, "a1");
        User b1 = new User(2, "b1");
        User b2 = new User(3, "b2");
        User c1 = new User(4, "c1");
        registerUsers(a1, b1, b2, c1);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[2,3]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0), true);
        ApprovalNode cNode = approvalNode("C审批", "[4]", null, true);
        Workflow workflow = saveWorkflow(a1, startNode, bNode, cNode, EndNode.builder().build());
        Map<String, Object> data = data();

        // when：流程提交到 B 节点
        startAndSubmit(workflow, startNode, a1, data);
        FlowContent bDetail = detail(todoOf(b1, bNode), b1);

        // then：B 节点只能退回开始节点
        assertEquals(List.of(startNode.getId()), backNodeIds(bDetail), "B 节点只应能退回开始节点");

        // when：b1 审批通过，流程提交到 C 节点
        pass(todoOf(b1, bNode), bNode, b1, data);
        FlowContent cDetail = detail(todoOf(c1, cNode), c1);

        // then：C 节点可退回 B、A，且最近的 B 排第一，不包含当前节点与结束节点
        assertEquals(List.of(bNode.getId(), startNode.getId()), backNodeIds(cDetail),
                "C 节点可退回节点应为 B、A（最近优先）");
        assertEquals(List.of("B审批", "开始节点"), backNodeNames(cDetail));
    }

    /**
     * 条件分支：未执行的分支节点不能作为退回目标，也不应出现在可退回列表中。
     */
    @Test
    void shouldRejectBackNodeNotExecutedInConditionBranch() {
        // given：A -> B -> 条件(days<=3 走 D，否则走 E) -> F -> 结束，本次数据命中 D 分支
        User a1 = new User(1, "a1");
        User b1 = new User(2, "b1");
        User d1 = new User(3, "d1");
        User e1 = new User(4, "e1");
        User f1 = new User(5, "f1");
        registerUsers(a1, b1, d1, e1, f1);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[2]", null, true);
        ApprovalNode dNode = approvalNode("D审批", "[3]", null, true);
        ApprovalNode eNode = approvalNode("E审批", "[4]", null, true);
        ApprovalNode fNode = approvalNode("F审批", "[5]", null, true);

        ConditionBranchNode hitBranch = ConditionBranchNode.builder()
                .name("命中分支")
                .conditionScript(FlowGroovyScriptFactory.createConditionScript(
                        "def run(request){return request.getFormData('days') <= 3}").getKey())
                .order(1)
                .blocks(dNode)
                .build();
        ConditionElseBranchNode elseBranch = ConditionElseBranchNode.builder()
                .name("else分支")
                .order(2)
                .blocks(eNode)
                .build();
        ConditionNode conditionNode = ConditionNode.builder()
                .name("条件控制")
                .blocks(hitBranch, elseBranch)
                .build();

        Workflow workflow = saveWorkflow(a1, startNode, bNode, conditionNode, fNode,
                EndNode.builder().build());
        Map<String, Object> data = data();

        startAndSubmit(workflow, startNode, a1, data);
        pass(todoOf(b1, bNode), bNode, b1, data);
        pass(todoOf(d1, dNode), dNode, d1, data);
        FlowRecord fTodo = todoOf(f1, fNode);

        // then 1：可退回节点只包含执行过的 D、B、A，未执行的 E 不在其中
        FlowContent fDetail = detail(fTodo, f1);
        assertEquals(List.of("D审批", "B审批", "开始节点"), backNodeNames(fDetail),
                "未执行的分支节点不应出现在可退回列表中");

        // then 2：即使绕过前端直接指定未执行过的 E，也必须被拒绝
        assertThrows(FlowException.class,
                () -> returnTo(fTodo, fNode, f1, eNode, data),
                "退回到未执行过的节点必须被拒绝");
    }

    /**
     * 开始节点即使被配置了退回动作，也不应对外暴露退回按钮与候选节点。
     */
    @Test
    void startNodeShouldNotExposeReturnAction() {
        // given：A -> B -> D，并在开始节点的流程定义 JSON 中写入 RETURN（模拟设计器按钮配置）
        User a1 = new User(1, "a1");
        User b1 = new User(2, "b1");
        registerUsers(a1, b1);

        StartNode startNode = startNode();
        ApprovalNode bNode = approvalNode("B审批", "[2]", null, false);
        Workflow workflow = saveWorkflow(a1, startNode, bNode, EndNode.builder().build());

        JSONObject json = JSONObject.parseObject(workflow.toJson());
        JSONArray nodes = json.getJSONArray("nodes");
        JSONObject startJson = nodes.getJSONObject(0);
        JSONObject returnAction = new JSONObject();
        returnAction.put("id", "start-return-action");
        returnAction.put("type", ActionType.RETURN.name());
        returnAction.put("title", "退回");
        returnAction.put("enable", true);
        startJson.getJSONArray("actions").add(returnAction);
        Workflow parsed = Workflow.formJson(json.toJSONString());
        factory.workflowService.saveWorkflow(parsed);

        // when：发起流程后停留在开始节点
        FlowRecord startTodo = create(parsed, a1, data());
        FlowContent detail = detail(startTodo, a1);

        // then 1：开始节点不展示退回动作，也没有可退回节点
        assertTrue(detail.getBackNodes() == null || detail.getBackNodes().isEmpty(),
                "开始节点不应有可退回节点");
        assertFalse(detail.getActions().stream()
                        .anyMatch(action -> ActionType.RETURN.name().equals(action.get("type"))),
                "开始节点不应下发退回按钮");
        assertFalse(parsed.getStartNode().actionManager().getActions().stream()
                        .noneMatch(action -> ActionType.RETURN.name().equals(action.type())),
                "前置条件：开始节点确实配置了退回动作");

        // then 2：直接调用退回接口同样被拒绝
        IFlowNode parsedStartNode = parsed.getStartNode();
        IFlowNode parsedBNode = parsed.getFlowNode(bNode.getId());
        assertThrows(FlowException.class, () -> returnTo(startTodo, parsedStartNode, a1, parsedBNode, data()),
                "开始节点发起退回必须被拒绝");
    }

    // ==================== 流程装配 ====================

    private void registerUsers(User... users) {
        for (User user : users) {
            factory.userGateway.save(user);
        }
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);
    }

    private FlowForm form() {
        return FlowFormBuilder.builder()
                .name("issue270")
                .code("issue270")
                .addField("name", "name", DataType.STRING)
                .addField("days", "days", DataType.INTEGER)
                .build();
    }

    private Map<String, Object> data() {
        return Map.of("name", "lorne", "days", 1);
    }

    private FormFieldPermissionStrategy perm() {
        return new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                .addPermission("issue270", "name", PermissionType.WRITE)
                .addPermission("issue270", "days", PermissionType.WRITE)
                .build());
    }

    private StartNode startNode() {
        return StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(perm())
                        .build())
                .build();
    }

    private ApprovalNode approvalNode(String name, String operatorIds,
                                      MultiOperatorAuditStrategy auditStrategy, boolean withReturn) {
        NodeStrategyBuilder strategyBuilder = NodeStrategyBuilder.builder()
                .addStrategy(perm())
                .addStrategy(new OperatorLoadStrategy(
                        FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return " + operatorIds + "}")
                                .getKey()));
        if (auditStrategy != null) {
            strategyBuilder.addStrategy(auditStrategy);
        }
        ActionBuilder actionBuilder = ActionBuilder.builder().addAction(PassAction.defaultAction());
        if (withReturn) {
            actionBuilder.addAction(ReturnAction.defaultAction());
        }
        return ApprovalNode.builder()
                .name(name)
                .strategies(strategyBuilder.build())
                .actions(actionBuilder.build())
                .build();
    }

    private Workflow saveWorkflow(User creator, IFlowNode... nodes) {
        WorkflowBuilder builder = WorkflowBuilder.builder()
                .title("issue270")
                .code("issue270")
                .createdOperator(creator)
                .form(form());
        for (IFlowNode node : nodes) {
            builder.addNode(node);
        }
        Workflow workflow = builder.build();
        factory.workflowService.saveWorkflow(workflow);
        return workflow;
    }

    // ==================== 操作与断言 ====================

    private FlowRecord create(Workflow workflow, User operator, Map<String, Object> data) {
        List<IFlowAction> startActions = workflow.getStartNode().actionManager().getActions();
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(operator.getUserId());
        factory.flowService.create(createRequest);
        return todoOf(operator, workflow.getStartNode());
    }

    private void startAndSubmit(Workflow workflow, StartNode startNode, User user, Map<String, Object> data) {
        create(workflow, user, data);
        pass(todoOf(user, startNode), startNode, user, data);
    }

    private void pass(FlowRecord record, IFlowNode node, User operator, Map<String, Object> data) {
        IFlowAction action = node.actionManager().getActions().stream()
                .filter(item -> ActionType.PASS.name().equals(item.type()))
                .findFirst().orElseThrow();
        flowAction(record, operator, action, data, "同意");
    }

    private void returnTo(FlowRecord record, IFlowNode node, User operator,
                          IFlowNode backNode, Map<String, Object> data) {
        IFlowAction action = node.actionManager().getActions().stream()
                .filter(item -> ActionType.RETURN.name().equals(item.type()))
                .findFirst().orElseThrow();
        FlowActionRequest request = new FlowActionRequest();
        request.setFormData(data);
        request.setRecordId(record.getId());
        FlowAdviceBody adviceBody = new FlowAdviceBody(action.id(), "退回", operator.getUserId());
        adviceBody.setBackNodeId(backNode.getId());
        request.setAdvice(adviceBody);
        factory.flowService.action(request);
    }

    private void flowAction(FlowRecord record, User operator, IFlowAction action,
                            Map<String, Object> data, String advice) {
        FlowActionRequest request = new FlowActionRequest();
        request.setFormData(data);
        request.setRecordId(record.getId());
        request.setAdvice(new FlowAdviceBody(action.id(), advice, operator.getUserId()));
        factory.flowService.action(request);
    }

    private FlowContent detail(FlowRecord record, User operator) {
        return factory.flowService.detail(new FlowDetailRequest(record.getId(), operator.getUserId()));
    }

    private List<String> backNodeIds(FlowContent detail) {
        return detail.getBackNodes().stream().map(NodeOption::getId).toList();
    }

    private List<String> backNodeNames(FlowContent detail) {
        return detail.getBackNodes().stream().map(NodeOption::getName).toList();
    }

    private FlowRecord todoOf(User user, IFlowNode node) {
        List<FlowRecord> list = factory.flowRecordRepository.findTodoByOperator(user.getUserId()).stream()
                .filter(record -> node.getId().equals(record.getNodeId()))
                .toList();
        assertEquals(1, list.size(), user.getName() + " 在 " + node.getName() + " 应有且仅有一条待办");
        return list.get(0);
    }
}

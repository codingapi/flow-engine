package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.builder.ActionBuilder;
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
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FlowInitiatorSelectProcessNodeTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    /**
     * 流程记录节点审批人展示验证。
     *
     * <p>流程设计：A(开始节点) -> B(发起人设定为 b) -> C(发起人设定为 c) -> D(结束节点)。
     * <p>操作步骤：a 发起流程，在提交开始节点时分别为 B、C 指定审批人；流程到达 B 后查询流程节点记录。
     * <p>期望：B 当前待办下返回的 C 节点审批人应为发起时指定的 c，不能错误展示为当前审批人 b。
     */
    @Test
    void processNodes_shouldShowInitiatorSelectedNextApprovalNodeOperator() {
        User a = new User(1, "a");
        User b = new User(2, "b");
        User c = new User(3, "c");
        factory.userGateway.save(a);
        factory.userGateway.save(b);
        factory.userGateway.save(c);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("审批流程")
                .code("initiator-select-approval")
                .addField("标题", "title", DataType.STRING)
                .build();

        StartNode startNode = StartNode.builder()
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();

        ApprovalNode bNode = ApprovalNode.builder()
                .name("B审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(OperatorLoadStrategy.initiatorSelectStrategy())
                        .build())
                .build();

        ApprovalNode cNode = ApprovalNode.builder()
                .name("C审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(OperatorLoadStrategy.initiatorSelectStrategy())
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("审批流程")
                .code("initiator-select-approval")
                .createdOperator(a)
                .form(form)
                .addNode(startNode)
                .addNode(bNode)
                .addNode(cNode)
                .addNode(endNode)
                .build();
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("title", "发起人设定审批人");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(a.getUserId());
        factory.flowService.create(createRequest);

        List<FlowRecord> aRecordList = factory.flowRecordRepository.findTodoByOperator(a.getUserId());
        assertEquals(1, aRecordList.size());

        FlowActionRequest aRequest = new FlowActionRequest();
        aRequest.setFormData(data);
        aRequest.setRecordId(aRecordList.get(0).getId());
        FlowAdviceBody advice = new FlowAdviceBody(startActions.get(0).id(), "提交", a.getUserId());
        advice.setOperatorSelectMap(Map.of(
                bNode.getId(), List.of(b.getUserId()),
                cNode.getId(), List.of(c.getUserId())
        ));
        aRequest.setAdvice(advice);
        factory.flowService.action(aRequest);

        List<FlowRecord> bRecordList = factory.flowRecordRepository.findTodoByOperator(b.getUserId());
        assertEquals(1, bRecordList.size());

        List<ProcessNode> nodeList = factory.flowService.processNodes(
                new FlowProcessNodeRequest(bRecordList.get(0).getId(), b.getUserId(), data));
        ProcessNode cProcessNode = nodeList.stream()
                .filter(node -> cNode.getId().equals(node.getNodeId()))
                .findFirst()
                .orElse(null);

        assertNotNull(cProcessNode);
        assertEquals(ProcessNode.ApproveState.PENDING, cProcessNode.getApproveState());
        assertEquals(1, cProcessNode.getOperators().size());
        assertEquals(c.getUserId(), cProcessNode.getOperators().get(0).getFlowOperator().getUserId());
        assertEquals(c.getName(), cProcessNode.getOperators().get(0).getFlowOperator().getName());
    }

    /**
     * 流程记录节点审批人展示验证。
     *
     * <p>流程设计：A(开始节点) -> B(审批人 b) -> C(由 B 审批时设定为 c) -> D(结束节点)。
     * <p>操作步骤：a 提交到 b，b 审批时为 C 指定审批人 c，再查询流程节点记录。
     * <p>期望：C 节点审批人应为 b 审批时指定的 c。
     */
    @Test
    void processNodes_shouldShowApproverSelectedCurrentApprovalNodeOperator() {
        User a = new User(1, "a");
        User b = new User(2, "b");
        User c = new User(3, "c");
        factory.userGateway.save(a);
        factory.userGateway.save(b);
        factory.userGateway.save(c);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("审批流程")
                .code("approver-select-approval")
                .addField("标题", "title", DataType.STRING)
                .build();

        StartNode startNode = StartNode.builder()
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();

        ApprovalNode bNode = ApprovalNode.builder()
                .name("B审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .build())
                .build();

        ApprovalNode cNode = ApprovalNode.builder()
                .name("C审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(OperatorLoadStrategy.approverSelectStrategy())
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("审批流程")
                .code("approver-select-approval")
                .createdOperator(a)
                .form(form)
                .addNode(startNode)
                .addNode(bNode)
                .addNode(cNode)
                .addNode(endNode)
                .build();
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("title", "审批人设定审批人");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(a.getUserId());
        factory.flowService.create(createRequest);

        List<FlowRecord> aRecordList = factory.flowRecordRepository.findTodoByOperator(a.getUserId());
        assertEquals(1, aRecordList.size());

        FlowActionRequest aRequest = new FlowActionRequest();
        aRequest.setFormData(data);
        aRequest.setRecordId(aRecordList.get(0).getId());
        aRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "提交", a.getUserId()));
        factory.flowService.action(aRequest);

        List<FlowRecord> bRecordList = factory.flowRecordRepository.findTodoByOperator(b.getUserId());
        assertEquals(1, bRecordList.size());

        List<IFlowAction> bActions = bNode.actionManager().getActions();
        FlowActionRequest bRequest = new FlowActionRequest();
        bRequest.setFormData(data);
        bRequest.setRecordId(bRecordList.get(0).getId());
        FlowAdviceBody advice = new FlowAdviceBody(bActions.get(0).id(), "同意", b.getUserId());
        advice.setOperatorSelectMap(Map.of(cNode.getId(), List.of(c.getUserId())));
        bRequest.setAdvice(advice);
        factory.flowService.action(bRequest);

        List<ProcessNode> nodeList = factory.flowService.processNodes(
                new FlowProcessNodeRequest(bRecordList.get(0).getId(), b.getUserId(), data));
        ProcessNode cProcessNode = nodeList.stream()
                .filter(node -> cNode.getId().equals(node.getNodeId()))
                .findFirst()
                .orElse(null);

        assertNotNull(cProcessNode);
        assertEquals(ProcessNode.ApproveState.PROCESSING, cProcessNode.getApproveState());
        assertEquals(1, cProcessNode.getOperators().size());
        assertEquals(c.getUserId(), cProcessNode.getOperators().get(0).getFlowOperator().getUserId());
        assertEquals(c.getName(), cProcessNode.getOperators().get(0).getFlowOperator().getName());
    }
}

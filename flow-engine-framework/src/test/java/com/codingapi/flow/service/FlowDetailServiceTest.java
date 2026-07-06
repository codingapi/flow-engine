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
import com.codingapi.flow.node.nodes.NotifyNode;
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
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class FlowDetailServiceTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();


    /**
     * 流程详情
     */
    @Test
    void detail() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        FlowContent detail = factory.flowService.detail(new FlowDetailRequest(workflow.getCode(), user.getUserId()));
        assertEquals(detail.getForm().getCode(), form.getCode());
        assertEquals(detail.getActions().size(), startNode.actionManager().getActions().size());
        assertNull(detail.getTodos());
        assertNull(detail.getHistories());


        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkCode(workflow.getCode());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        detail = factory.flowService.detail(new FlowDetailRequest(userRecordList.get(0).getId(), user.getUserId()));
        assertEquals(detail.getForm().getCode(), form.getCode());
        assertEquals(detail.getActions().size(), startNode.actionManager().getActions().size());
        assertEquals(1, detail.getTodos().size());
        assertEquals(0, detail.getHistories().size());
        assertEquals(data, detail.getTodos().get(0).getData());


        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        detail = factory.flowService.detail(new FlowDetailRequest(bossRecordList.get(0).getId(),boss.getUserId()));
        assertEquals(detail.getForm().getCode(), form.getCode());
        assertEquals(detail.getActions().size(), bossNode.actionManager().getActions().size());
        assertEquals(1, detail.getTodos().size());
        assertEquals(1, detail.getHistories().size());
        assertEquals(data, detail.getTodos().get(0).getData());

        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }




    /**
     * 流程详情
     */
    @Test
    void processNodes() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .build()
                )
                .build();


        NotifyNode notifyNode = NotifyNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(notifyNode)
                .addNode(endNode)
                .build();

         factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<ProcessNode> nodeList = factory.flowService.processNodes(new FlowProcessNodeRequest(workflow.getCode(), user.getUserId(),data));
        assertEquals(4,nodeList.size());
        assertEquals(0,nodeList.stream().filter(ProcessNode::isHistory).toList().size());

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkCode(workflow.getCode());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        nodeList = factory.flowService.processNodes(new FlowProcessNodeRequest(userRecordList.get(0).getId(), user.getUserId(),data));
        assertEquals(4,nodeList.size());
        assertEquals(0,nodeList.stream().filter(ProcessNode::isHistory).toList().size());


        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        nodeList = factory.flowService.processNodes(new FlowProcessNodeRequest(bossRecordList.get(0).getId(), boss.getUserId(),data));
        assertEquals(4,nodeList.size());
        assertEquals(1,nodeList.stream().filter(ProcessNode::isHistory).toList().size());

        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());


        nodeList = factory.flowService.processNodes(new FlowProcessNodeRequest(bossRecordList.get(0).getId(), boss.getUserId(),data));
        assertEquals(4,nodeList.size());

    }


    /**
     * 流程记录节点审批人展示验证。
     *
     * <p>流程设计：A(开始节点) -> B(审批人 b) -> C(审批人 c) -> D(结束节点)。
     * <p>操作步骤：a 发起流程并提交到 b，在 b 的当前待办记录下查询流程节点记录。
     * <p>期望：未审批的 C 节点审批人应展示为 c，不能错误展示为当前审批人 b。
     */
    @Test
    void processNodes_shouldShowNextApprovalNodeOperator() {

        User a = new User(1, "a");
        User b = new User(2, "b");
        User c = new User(3, "c");
        factory.userGateway.save(a);
        factory.userGateway.save(b);
        factory.userGateway.save(c);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("审批流程")
                .code("approval")
                .addField("标题", "title", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();

        ApprovalNode bNode = ApprovalNode.builder()
                .name("B审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .build()
                )
                .build();

        ApprovalNode cNode = ApprovalNode.builder()
                .name("C审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [3]}").getKey()))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("审批流程")
                .code("approval")
                .createdOperator(a)
                .form(form)
                .addNode(startNode)
                .addNode(bNode)
                .addNode(cNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("title", "test");

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startNode.actionManager().getActions().get(0).id());
        createRequest.setOperatorId(a.getUserId());
        factory.flowService.create(createRequest);

        List<FlowRecord> aRecordList = factory.flowRecordRepository.findTodoByOperator(a.getUserId());
        assertEquals(1, aRecordList.size());

        FlowActionRequest aRequest = new FlowActionRequest();
        aRequest.setFormData(data);
        aRequest.setRecordId(aRecordList.get(0).getId());
        aRequest.setAdvice(new FlowAdviceBody(startNode.actionManager().getActions().get(0).id(), "提交", a.getUserId()));
        factory.flowService.action(aRequest);

        List<FlowRecord> bRecordList = factory.flowRecordRepository.findTodoByOperator(b.getUserId());
        assertEquals(1, bRecordList.size());

        List<ProcessNode> nodeList = factory.flowService.processNodes(new FlowProcessNodeRequest(bRecordList.get(0).getId(), b.getUserId(), data));
        ProcessNode nextApprovalNode = nodeList.stream()
                .filter(node -> cNode.getId().equals(node.getNodeId()))
                .findFirst()
                .orElse(null);

        assertNotNull(nextApprovalNode);
        assertEquals(ProcessNode.ApproveState.PENDING, nextApprovalNode.getApproveState());
        assertEquals(1, nextApprovalNode.getOperators().size());
        assertEquals(c.getUserId(), nextApprovalNode.getOperators().get(0).getFlowOperator().getUserId());
        assertEquals(c.getName(), nextApprovalNode.getOperators().get(0).getFlowOperator().getName());
    }
}

package com.codingapi.flow.service;

import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.response.ActionResponse;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.OperatorSelectType;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 操作人手动选择功能测试
 */
class OperatorSelectTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    /**
     * 测试发起人设定模式（INITIATOR_SELECT）- 未提供操作人时返回提示
     * 第一次提交不带 operatorSelectMap，验证返回 OPERATOR_SELECT 响应
     * 第二次提交带上 operatorSelectMap，验证流程正常推进
     */
    @Test
    void testInitiatorSelectPrompt() {
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

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .build();

        // 审批节点使用 INITIATOR_SELECT 模式
        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.READ)
                                .addPermission("leave", "days", PermissionType.READ)
                                .addPermission("leave", "reason", PermissionType.READ)
                                .build()))
                        .addStrategy(OperatorLoadStrategy.initiatorSelectStrategy())
                        .build())
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

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        // 发起流程（不提供 operatorSelectMap）
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(user.getUserId());
        factory.flowService.create(createRequest);

        // 用户提交开始节点（不提供 operatorSelectMap）
        List<FlowRecord> userRecords = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecords.size());

        FlowActionRequest userAction = new FlowActionRequest();
        userAction.setFormData(data);
        userAction.setRecordId(userRecords.get(0).getId());
        userAction.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "提交", user.getUserId()));
        ActionResponse response = factory.flowService.action(userAction);

        // 验证：返回 OPERATOR_SELECT 类型的响应，提示设定操作人
        assertNotNull(response);
        assertEquals(ActionResponse.ResponseType.OPERATOR_SELECT, response.getResponseType());
        assertEquals(1, response.getOptions().size());
        assertEquals(bossNode.getId(), response.getOptions().get(0).getId());
        assertEquals("经理审批", response.getOptions().get(0).getName());

        // 第二次提交：带上 operatorSelectMap
        FlowActionRequest userAction2 = new FlowActionRequest();
        userAction2.setFormData(data);
        userAction2.setRecordId(userRecords.get(0).getId());
        FlowAdviceBody adviceWithOperators = new FlowAdviceBody(startActions.get(0).id(), "提交", user.getUserId());
        adviceWithOperators.setOperatorSelectMap(Map.of(bossNode.getId(), List.of(boss.getUserId())));
        userAction2.setAdvice(adviceWithOperators);
        ActionResponse response2 = factory.flowService.action(userAction2);

        // 验证：第二次提交正常通过，无提示返回
        assertNull(response2);

        // 验证：boss 收到了待办
        List<FlowRecord> bossRecords = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecords.size());

        // boss 审批通过
        List<IFlowAction> bossActions = bossNode.actionManager().getActions();
        FlowActionRequest bossAction = new FlowActionRequest();
        bossAction.setFormData(data);
        bossAction.setRecordId(bossRecords.get(0).getId());
        bossAction.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossAction);

        // 验证流程结束
        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecords.get(0).getProcessId());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());
    }


    /**
     * 测试审批人设定模式（APPROVER_SELECT）- 未提供操作人时返回提示
     * boss 审批时未指定总监审批节点操作人，验证返回提示
     * 再次提交带上 operatorSelectMap，验证流程正常推进
     */
    @Test
    void testApproverSelectPrompt() {
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User director = new User(3, "director");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(director);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .build();

        // 经理审批节点使用脚本模式
        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.READ)
                                .addPermission("leave", "days", PermissionType.READ)
                                .addPermission("leave", "reason", PermissionType.READ)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .build())
                .build();

        // 总监审批节点使用 APPROVER_SELECT 模式
        ApprovalNode directorNode = ApprovalNode.builder()
                .name("总监审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.READ)
                                .addPermission("leave", "days", PermissionType.READ)
                                .addPermission("leave", "reason", PermissionType.READ)
                                .build()))
                        .addStrategy(OperatorLoadStrategy.approverSelectStrategy())
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(directorNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 5, "reason", "leave");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        // 发起流程
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(user.getUserId());
        factory.flowService.create(createRequest);

        // 用户提交开始节点
        List<FlowRecord> userRecords = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecords.size());

        FlowActionRequest userAction = new FlowActionRequest();
        userAction.setFormData(data);
        userAction.setRecordId(userRecords.get(0).getId());
        userAction.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "提交", user.getUserId()));
        factory.flowService.action(userAction);

        // boss 收到待办
        List<FlowRecord> bossRecords = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecords.size());

        // boss 第一次审批（不提供 operatorSelectMap）
        List<IFlowAction> bossActions = bossNode.actionManager().getActions();
        FlowActionRequest bossAction1 = new FlowActionRequest();
        bossAction1.setFormData(data);
        bossAction1.setRecordId(bossRecords.get(0).getId());
        bossAction1.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        ActionResponse response = factory.flowService.action(bossAction1);

        // 验证：返回 OPERATOR_SELECT 类型的响应，提示设定操作人
        assertNotNull(response);
        assertEquals(ActionResponse.ResponseType.OPERATOR_SELECT, response.getResponseType());
        assertEquals(1, response.getOptions().size());
        assertEquals(directorNode.getId(), response.getOptions().get(0).getId());
        assertEquals("总监审批", response.getOptions().get(0).getName());

        // boss 第二次审批（带上 operatorSelectMap）
        FlowActionRequest bossAction2 = new FlowActionRequest();
        bossAction2.setFormData(data);
        bossAction2.setRecordId(bossRecords.get(0).getId());
        FlowAdviceBody bossAdvice = new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId());
        bossAdvice.setOperatorSelectMap(Map.of(directorNode.getId(), List.of(director.getUserId())));
        bossAction2.setAdvice(bossAdvice);
        ActionResponse response2 = factory.flowService.action(bossAction2);

        // 验证：第二次提交正常通过
        assertNull(response2);

        // 验证：director 收到了待办
        List<FlowRecord> directorRecords = factory.flowRecordRepository.findTodoByOperator(director.getUserId());
        assertEquals(1, directorRecords.size());

        // director 审批通过
        List<IFlowAction> directorActions = directorNode.actionManager().getActions();
        FlowActionRequest directorAction = new FlowActionRequest();
        directorAction.setFormData(data);
        directorAction.setRecordId(directorRecords.get(0).getId());
        directorAction.setAdvice(new FlowAdviceBody(directorActions.get(0).id(), "同意", director.getUserId()));
        factory.flowService.action(directorAction);

        // 验证流程结束
        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(directorRecords.get(0).getProcessId());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());
    }


    /**
     * 测试发起人设定模式 - 直接提供操作人（无提示）
     * 在创建流程时就已提供 operatorSelectMap，不应触发提示
     */
    @Test
    void testInitiatorSelectDirectProvide() {
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

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.READ)
                                .addPermission("leave", "days", PermissionType.READ)
                                .addPermission("leave", "reason", PermissionType.READ)
                                .build()))
                        .addStrategy(OperatorLoadStrategy.initiatorSelectStrategy())
                        .build())
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

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        // 发起流程时就提供 operatorSelectMap
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(user.getUserId());
        createRequest.setOperatorSelectMap(Map.of(bossNode.getId(), List.of(boss.getUserId())));
        factory.flowService.create(createRequest);

        // 用户提交开始节点（在 action 中也带上 operatorSelectMap）
        List<FlowRecord> userRecords = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecords.size());

        FlowActionRequest userAction = new FlowActionRequest();
        userAction.setFormData(data);
        userAction.setRecordId(userRecords.get(0).getId());
        FlowAdviceBody adviceBody = new FlowAdviceBody(startActions.get(0).id(), "提交", user.getUserId());
        adviceBody.setOperatorSelectMap(Map.of(bossNode.getId(), List.of(boss.getUserId())));
        userAction.setAdvice(adviceBody);
        ActionResponse response = factory.flowService.action(userAction);

        // 验证：正常通过，无提示
        assertNull(response);

        // boss 收到了待办
        List<FlowRecord> bossRecords = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecords.size());
    }


    /**
     * 测试脚本模式的向后兼容性
     * 确保旧的脚本模式流程依然正常工作
     */
    @Test
    void testScriptBackwardCompatibility() {
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

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .build();

        // 使用传统脚本方式指定操作人
        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.READ)
                                .addPermission("leave", "days", PermissionType.READ)
                                .addPermission("leave", "reason", PermissionType.READ)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .build())
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

        // 验证 OperatorLoadStrategy 的序列化和反序列化
        OperatorLoadStrategy scriptStrategy = new OperatorLoadStrategy(FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey());
        Map<String, Object> map = scriptStrategy.toMap();
        assertEquals("SCRIPT", map.get("selectType"));

        OperatorLoadStrategy deserialized = OperatorLoadStrategy.fromMap(map);
        assertNotNull(deserialized);
        assertEquals(OperatorSelectType.SCRIPT, deserialized.getSelectType());

        // 验证向后兼容：没有 selectType 字段时默认为 SCRIPT
        map.remove("selectType");
        OperatorLoadStrategy backwardCompatible = OperatorLoadStrategy.fromMap(map);
        assertNotNull(backwardCompatible);
        assertEquals(OperatorSelectType.SCRIPT, backwardCompatible.getSelectType());

        // 验证流程正常工作
        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(user.getUserId());
        factory.flowService.create(createRequest);

        List<FlowRecord> userRecords = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecords.size());

        FlowActionRequest userAction = new FlowActionRequest();
        userAction.setFormData(data);
        userAction.setRecordId(userRecords.get(0).getId());
        userAction.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "提交", user.getUserId()));
        ActionResponse response = factory.flowService.action(userAction);

        // 验证：脚本模式不会触发操作人选择提示
        assertNull(response);

        // boss 通过脚本模式收到待办
        List<FlowRecord> bossRecords = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecords.size());
    }


    /**
     * 测试发起人设定模式 - 配置可选人员范围
     * 范围脚本返回 [boss, director]，OPERATOR_SELECT 响应回传候选范围；选择范围内的 boss 应正常通过
     */
    @Test
    void testInitiatorSelectWithRange() {
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User director = new User(3, "director");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(director);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder().addStrategy(writePermission()).build())
                .build();

        // 审批节点使用 INITIATOR_SELECT 模式，并配置可选人员范围脚本（boss、director）
        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(readPermission())
                        .addStrategy(OperatorLoadStrategy.initiatorSelectStrategy(FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2,3]}").getKey()))
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程").code("leave").createdOperator(user).form(leaveForm())
                .addNode(startNode).addNode(bossNode).addNode(endNode).build();
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(user.getUserId());
        factory.flowService.create(createRequest);

        List<FlowRecord> userRecords = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecords.size());

        // 第一次提交不带 operatorSelectMap，验证返回候选范围
        FlowActionRequest userAction = new FlowActionRequest();
        userAction.setFormData(data);
        userAction.setRecordId(userRecords.get(0).getId());
        userAction.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "提交", user.getUserId()));
        ActionResponse response = factory.flowService.action(userAction);

        assertNotNull(response);
        assertEquals(ActionResponse.ResponseType.OPERATOR_SELECT, response.getResponseType());
        assertEquals(1, response.getOptions().size());
        // 候选范围随响应回传：boss、director 两人
        assertNotNull(response.getOptions().get(0).getOperators());
        assertEquals(2, response.getOptions().get(0).getOperators().size());

        // 第二次提交选择范围内的 boss，正常通过
        FlowActionRequest userAction2 = new FlowActionRequest();
        userAction2.setFormData(data);
        userAction2.setRecordId(userRecords.get(0).getId());
        FlowAdviceBody advice = new FlowAdviceBody(startActions.get(0).id(), "提交", user.getUserId());
        advice.setOperatorSelectMap(Map.of(bossNode.getId(), List.of(boss.getUserId())));
        userAction2.setAdvice(advice);
        ActionResponse response2 = factory.flowService.action(userAction2);

        assertNull(response2);
        List<FlowRecord> bossRecords = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecords.size());
    }


    /**
     * 测试发起人设定模式 - 选择超出范围的人员应报错
     * 范围脚本仅允许 boss(2)，选择 director(3) 时应抛出校验异常
     */
    @Test
    void testInitiatorSelectOutOfRange() {
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User director = new User(3, "director");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(director);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder().addStrategy(writePermission()).build())
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(readPermission())
                        .addStrategy(OperatorLoadStrategy.initiatorSelectStrategy(FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程").code("leave").createdOperator(user).form(leaveForm())
                .addNode(startNode).addNode(bossNode).addNode(endNode).build();
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(user.getUserId());
        factory.flowService.create(createRequest);

        List<FlowRecord> userRecords = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecords.size());

        // 提交时选择范围外的 director，应报错
        FlowActionRequest userAction = new FlowActionRequest();
        userAction.setFormData(data);
        userAction.setRecordId(userRecords.get(0).getId());
        FlowAdviceBody advice = new FlowAdviceBody(startActions.get(0).id(), "提交", user.getUserId());
        advice.setOperatorSelectMap(Map.of(bossNode.getId(), List.of(director.getUserId())));
        userAction.setAdvice(advice);

        assertThrows(FlowValidationException.class, () -> factory.flowService.action(userAction));
    }


    /**
     * 测试审批人设定模式 - 选择超出范围的人员应报错
     * 总监审批节点范围脚本仅允许 director(3)，boss 选择 user(1) 时应抛出校验异常
     */
    @Test
    void testApproverSelectOutOfRange() {
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User director = new User(3, "director");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(director);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder().addStrategy(writePermission()).build())
                .build();

        // 经理审批节点使用脚本模式固定 boss
        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(readPermission())
                        .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .build())
                .build();

        // 总监审批节点使用 APPROVER_SELECT 模式，范围仅允许 director
        ApprovalNode directorNode = ApprovalNode.builder()
                .name("总监审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(readPermission())
                        .addStrategy(OperatorLoadStrategy.approverSelectStrategy(FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [3]}").getKey()))
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程").code("leave").createdOperator(user).form(leaveForm())
                .addNode(startNode).addNode(bossNode).addNode(directorNode).addNode(endNode).build();
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 5, "reason", "leave");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(user.getUserId());
        factory.flowService.create(createRequest);

        // 用户提交开始节点
        List<FlowRecord> userRecords = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        FlowActionRequest userAction = new FlowActionRequest();
        userAction.setFormData(data);
        userAction.setRecordId(userRecords.get(0).getId());
        userAction.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "提交", user.getUserId()));
        factory.flowService.action(userAction);

        // boss 收到待办，选择范围外的 user(1)
        List<FlowRecord> bossRecords = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecords.size());

        List<IFlowAction> bossActions = bossNode.actionManager().getActions();
        FlowActionRequest bossAction = new FlowActionRequest();
        bossAction.setFormData(data);
        bossAction.setRecordId(bossRecords.get(0).getId());
        FlowAdviceBody bossAdvice = new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId());
        bossAdvice.setOperatorSelectMap(Map.of(directorNode.getId(), List.of(user.getUserId())));
        bossAction.setAdvice(bossAdvice);

        assertThrows(FlowValidationException.class, () -> factory.flowService.action(bossAction));
    }


    /**
     * 测试发起人设定模式 - 范围脚本返回空时视为不限范围
     * 脚本执行结果为空列表，等同未配置范围，可选任意人
     */
    @Test
    void testInitiatorSelectEmptyRangeAllowsAny() {
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder().addStrategy(writePermission()).build())
                .build();

        // 范围脚本返回空列表，视为不限范围
        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(readPermission())
                        .addStrategy(OperatorLoadStrategy.initiatorSelectStrategy(FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return []}").getKey()))
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程").code("leave").createdOperator(user).form(leaveForm())
                .addNode(startNode).addNode(bossNode).addNode(endNode).build();
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(user.getUserId());
        factory.flowService.create(createRequest);

        List<FlowRecord> userRecords = factory.flowRecordRepository.findTodoByOperator(user.getUserId());

        // 选择任意人 boss，范围为空不做限制，正常通过
        FlowActionRequest userAction = new FlowActionRequest();
        userAction.setFormData(data);
        userAction.setRecordId(userRecords.get(0).getId());
        FlowAdviceBody advice = new FlowAdviceBody(startActions.get(0).id(), "提交", user.getUserId());
        advice.setOperatorSelectMap(Map.of(bossNode.getId(), List.of(boss.getUserId())));
        userAction.setAdvice(advice);
        ActionResponse response = factory.flowService.action(userAction);

        assertNull(response);
        List<FlowRecord> bossRecords = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecords.size());
    }


    /**
     * 测试范围脚本的序列化与反序列化
     * INITIATOR/APPROVER 模式下范围脚本应可往返；未配置范围脚本时不写出 script
     */
    @Test
    void testRangeScriptSerialization() {
        OperatorLoadStrategy strategy = OperatorLoadStrategy.initiatorSelectStrategy("def run(request){return [2,3]}");
        Map<String, Object> map = strategy.toMap();
        assertEquals("INITIATOR_SELECT", map.get("selectType"));
        assertEquals("def run(request){return [2,3]}", map.get("script"));

        OperatorLoadStrategy restored = OperatorLoadStrategy.fromMap(map);
        assertNotNull(restored);
        assertEquals(OperatorSelectType.INITIATOR_SELECT, restored.getSelectType());

        // 未配置范围脚本时，不写出 script
        OperatorLoadStrategy noRange = OperatorLoadStrategy.approverSelectStrategy();
        Map<String, Object> noRangeMap = noRange.toMap();
        assertEquals("APPROVER_SELECT", noRangeMap.get("selectType"));
        assertFalse(noRangeMap.containsKey("script"));

        OperatorLoadStrategy restoredNoRange = OperatorLoadStrategy.fromMap(noRangeMap);
        assertNotNull(restoredNoRange);
        assertEquals(OperatorSelectType.APPROVER_SELECT, restoredNoRange.getSelectType());
    }


    /**
     * 审批人设定模式（APPROVER_SELECT）直接位于开始节点之后：发起人提交开始节点应提示设定操作人。
     *
     * <p>流程设计：A(发起) -> B(b1 审批人设定) -> C(c1 审批人设定) -> D(结束)。
     * <p>问题复现：B、C 均为单人审批节点且配置为审批人设定（APPROVER_SELECT），发起人提交开始节点后，
     * 既不返回 OPERATOR_SELECT 提示，也不流转到 B，而是在开始节点 A 上重复生成待办，流程始终停留在 A 节点。
     * <p>期望：开始节点直接下游为审批人设定节点时，发起人提交应返回 OPERATOR_SELECT 提示（由发起人指定 B 的审批人），
     * 指定后流程正常流转到 B；B 审批时再提示指定 C 的审批人，最终流程正常结束。
     */
    @Test
    void testApproverSelectDirectlyAfterStartNode() {
        User user = new User(1, "user");
        User b1 = new User(2, "b1");
        User c1 = new User(4, "c1");
        factory.userGateway.save(user);
        factory.userGateway.save(b1);
        factory.userGateway.save(c1);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder().addStrategy(writePermission()).build())
                .build();
        // B、C 均为单人审批节点，配置为审批人设定（APPROVER_SELECT）
        ApprovalNode bNode = ApprovalNode.builder()
                .name("B审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(readPermission())
                        .addStrategy(OperatorLoadStrategy.approverSelectStrategy())
                        .build())
                .build();
        ApprovalNode cNode = ApprovalNode.builder()
                .name("C审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(readPermission())
                        .addStrategy(OperatorLoadStrategy.approverSelectStrategy())
                        .build())
                .build();
        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程").code("leave").createdOperator(user).form(leaveForm())
                .addNode(startNode).addNode(bNode).addNode(cNode).addNode(endNode)
                .build();
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(user.getUserId());
        factory.flowService.create(createRequest);

        List<FlowRecord> userRecords = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecords.size());
        FlowRecord startTodo = userRecords.get(0);

        // ① 发起人提交开始节点（不带 operatorSelectMap）：应提示为 B 节点设定审批人
        FlowActionRequest startAction = new FlowActionRequest();
        startAction.setFormData(data);
        startAction.setRecordId(startTodo.getId());
        startAction.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "提交", user.getUserId()));
        ActionResponse response = factory.flowService.action(startAction);

        assertNotNull(response, "开始节点直接下游为审批人设定节点时，应返回 OPERATOR_SELECT 提示，而非流程停留在 A 节点");
        assertEquals(ActionResponse.ResponseType.OPERATOR_SELECT, response.getResponseType());
        assertEquals(1, response.getOptions().size());
        assertEquals(bNode.getId(), response.getOptions().get(0).getId());

        // 提示前后开始节点待办不应被消费或在 A 节点重复生成；B 节点尚不应产生待办
        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(user.getUserId()).size(),
                "返回设定审批人提示时，开始节点待办不应被消费，更不应在 A 节点重复生成待办");
        assertEquals(0, factory.flowRecordRepository.findTodoByOperator(b1.getUserId()).size());

        // ② 发起人再次提交并为 B 指定审批人 b1：流程流转到 B
        FlowActionRequest startAction2 = new FlowActionRequest();
        startAction2.setFormData(data);
        startAction2.setRecordId(startTodo.getId());
        FlowAdviceBody startAdvice = new FlowAdviceBody(startActions.get(0).id(), "提交", user.getUserId());
        startAdvice.setOperatorSelectMap(Map.of(bNode.getId(), List.of(b1.getUserId())));
        startAction2.setAdvice(startAdvice);
        assertNull(factory.flowService.action(startAction2));

        List<FlowRecord> b1Records = factory.flowRecordRepository.findTodoByOperator(b1.getUserId());
        assertEquals(1, b1Records.size(), "为 B 指定审批人 b1 后，流程应流转到 B 节点");
        assertEquals(0, factory.flowRecordRepository.findTodoByOperator(user.getUserId()).size());

        // ③ b1 审批（不带 operatorSelectMap）：应提示为 C 节点设定审批人
        List<IFlowAction> bActions = bNode.actionManager().getActions();
        FlowActionRequest bAction = new FlowActionRequest();
        bAction.setFormData(data);
        bAction.setRecordId(b1Records.get(0).getId());
        bAction.setAdvice(new FlowAdviceBody(bActions.get(0).id(), "同意", b1.getUserId()));
        ActionResponse bResponse = factory.flowService.action(bAction);

        assertNotNull(bResponse);
        assertEquals(ActionResponse.ResponseType.OPERATOR_SELECT, bResponse.getResponseType());
        assertEquals(cNode.getId(), bResponse.getOptions().get(0).getId());

        // ④ b1 再次审批并为 C 指定审批人 c1：流程流转到 C
        FlowActionRequest bAction2 = new FlowActionRequest();
        bAction2.setFormData(data);
        bAction2.setRecordId(b1Records.get(0).getId());
        FlowAdviceBody bAdvice = new FlowAdviceBody(bActions.get(0).id(), "同意", b1.getUserId());
        bAdvice.setOperatorSelectMap(Map.of(cNode.getId(), List.of(c1.getUserId())));
        bAction2.setAdvice(bAdvice);
        assertNull(factory.flowService.action(bAction2));

        List<FlowRecord> c1Records = factory.flowRecordRepository.findTodoByOperator(c1.getUserId());
        assertEquals(1, c1Records.size());

        // ⑤ c1 审批通过，流程结束
        List<IFlowAction> cActions = cNode.actionManager().getActions();
        FlowActionRequest cAction = new FlowActionRequest();
        cAction.setFormData(data);
        cAction.setRecordId(c1Records.get(0).getId());
        cAction.setAdvice(new FlowAdviceBody(cActions.get(0).id(), "同意", c1.getUserId()));
        factory.flowService.action(cAction);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(c1Records.get(0).getProcessId());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).count(), "流程应正常结束");
    }


    private FlowForm leaveForm() {
        return FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
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

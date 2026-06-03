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

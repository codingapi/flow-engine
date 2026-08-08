package com.codingapi.flow.service;

import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.AddAuditAction;
import com.codingapi.flow.action.actions.DelegateAction;
import com.codingapi.flow.action.actions.TransferAction;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.exception.FlowExecutionException;
import com.codingapi.flow.exception.FlowValidationException;
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
 * 选人数量限制（maxOperatorCount）功能测试
 * <p>
 * 覆盖：发起人/审批人设定的人数上限校验、转办/委托/加签的人数上限校验、序列化兼容。
 */
class OperatorMaxCountTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    private FlowForm leaveForm() {
        return FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();
    }

    private FormFieldPermissionStrategy permission() {
        return new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                .addPermission("leave", "name", PermissionType.WRITE)
                .addPermission("leave", "days", PermissionType.WRITE)
                .addPermission("leave", "reason", PermissionType.WRITE)
                .build());
    }

    /**
     * 发起人设定 - 选择人数超过上限（maxOperatorCount=1）应报错
     */
    @Test
    void testInitiatorSelectExceedMaxCount() {
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User director = new User(3, "director");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(director);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder().addStrategy(permission()).build())
                .build();

        // 发起人设定节点：可选范围内仅允许 1 人
        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(permission())
                        .addStrategy(OperatorLoadStrategy.initiatorSelectStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2,3]}").getKey(),
                                1))
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

        // 选择 2 人，超过 maxOperatorCount=1，应报错
        FlowActionRequest userAction = new FlowActionRequest();
        userAction.setFormData(data);
        userAction.setRecordId(userRecords.get(0).getId());
        FlowAdviceBody advice = new FlowAdviceBody(startActions.get(0).id(), "提交", user.getUserId());
        advice.setOperatorSelectMap(Map.of(bossNode.getId(), List.of(boss.getUserId(), director.getUserId())));
        userAction.setAdvice(advice);

        assertThrows(FlowValidationException.class, () -> factory.flowService.action(userAction));
    }

    /**
     * 审批人设定 - 选择人数超过上限（maxOperatorCount=1）应报错
     */
    @Test
    void testApproverSelectExceedMaxCount() {
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User director = new User(3, "director");
        User manager = new User(4, "manager");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(director);
        factory.userGateway.save(manager);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder().addStrategy(permission()).build())
                .build();

        // 经理脚本固定 boss
        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(permission())
                        .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .build())
                .build();

        // 总监审批人设定节点：仅允许 1 人
        ApprovalNode directorNode = ApprovalNode.builder()
                .name("总监审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(permission())
                        .addStrategy(OperatorLoadStrategy.approverSelectStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [3,4]}").getKey(),
                                1))
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

        List<FlowRecord> userRecords = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        FlowActionRequest userAction = new FlowActionRequest();
        userAction.setFormData(data);
        userAction.setRecordId(userRecords.get(0).getId());
        userAction.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "提交", user.getUserId()));
        factory.flowService.action(userAction);

        List<FlowRecord> bossRecords = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecords.size());

        List<IFlowAction> bossActions = bossNode.actionManager().getActions();
        // 选择 2 人，超过 maxOperatorCount=1，应报错
        FlowActionRequest bossAction = new FlowActionRequest();
        bossAction.setFormData(data);
        bossAction.setRecordId(bossRecords.get(0).getId());
        FlowAdviceBody bossAdvice = new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId());
        bossAdvice.setOperatorSelectMap(Map.of(directorNode.getId(), List.of(director.getUserId(), manager.getUserId())));
        bossAction.setAdvice(bossAdvice);

        assertThrows(FlowValidationException.class, () -> factory.flowService.action(bossAction));
    }

    /**
     * 发起人设定 - 未配置 maxOperatorCount（默认 -1）时选择任意数量均不限制
     */
    @Test
    void testInitiatorSelectDefaultNoLimit() {
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User director = new User(3, "director");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(director);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder().addStrategy(permission()).build())
                .build();

        // 无 maxOperatorCount 配置，默认不限制
        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(permission())
                        .addStrategy(OperatorLoadStrategy.initiatorSelectStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2,3]}").getKey()))
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

        // 选择 2 人，未配置上限，正常通过
        FlowActionRequest userAction = new FlowActionRequest();
        userAction.setFormData(data);
        userAction.setRecordId(userRecords.get(0).getId());
        FlowAdviceBody advice = new FlowAdviceBody(startActions.get(0).id(), "提交", user.getUserId());
        advice.setOperatorSelectMap(Map.of(bossNode.getId(), List.of(boss.getUserId(), director.getUserId())));
        userAction.setAdvice(advice);
        assertNull(factory.flowService.action(userAction));

        // 未配置上限时选择 2 人正常通过；默认顺序审批策略下仅第一个操作人（boss）的待办可见
        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(boss.getUserId()).size());
    }

    /**
     * 转办 - 选择人数超过上限（maxOperatorCount=1）应报错
     */
    @Test
    void testTransferExceedMaxCount() {
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User target1 = new User(3, "target1");
        User target2 = new User(4, "target2");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(target1);
        factory.userGateway.save(target2);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder().addStrategy(permission()).build())
                .build();

        ApprovalNode approvalNode = ApprovalNode.builder()
                .name("审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(permission())
                        .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .build())
                .build();
        // 转办人数上限 1
        TransferAction transferAction = (TransferAction) approvalNode.actionManager().getActionByType(ActionType.TRANSFER.name());
        transferAction.setMaxOperatorCount(1);

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程").code("leave").createdOperator(user).form(leaveForm())
                .addNode(startNode).addNode(approvalNode).addNode(endNode).build();
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
        FlowActionRequest userAction = new FlowActionRequest();
        userAction.setFormData(data);
        userAction.setRecordId(userRecords.get(0).getId());
        userAction.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "提交", user.getUserId()));
        factory.flowService.action(userAction);

        List<FlowRecord> bossRecords = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecords.size());

        // 转办给 2 人，超过 maxOperatorCount=1 应报错
        List<IFlowAction> bossActions = approvalNode.actionManager().getActions();
        IFlowAction transferAction2 = approvalNode.actionManager().getActionByType(ActionType.TRANSFER.name());
        FlowActionRequest transferRequest = new FlowActionRequest();
        transferRequest.setFormData(data);
        transferRequest.setRecordId(bossRecords.get(0).getId());
        FlowAdviceBody transferAdvice = new FlowAdviceBody(transferAction2.id(), boss.getUserId());
        transferAdvice.setForwardOperatorIds(List.of(target1.getUserId(), target2.getUserId()));
        transferRequest.setAdvice(transferAdvice);

        assertThrows(FlowExecutionException.class, () -> factory.flowService.action(transferRequest));
    }

    /**
     * 委托 - 选择人数超过上限（maxOperatorCount=1）应报错
     */
    @Test
    void testDelegateExceedMaxCount() {
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User target1 = new User(3, "target1");
        User target2 = new User(4, "target2");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(target1);
        factory.userGateway.save(target2);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder().addStrategy(permission()).build())
                .build();

        ApprovalNode approvalNode = ApprovalNode.builder()
                .name("审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(permission())
                        .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .build())
                .build();
        DelegateAction delegateAction = (DelegateAction) approvalNode.actionManager().getActionByType(ActionType.DELEGATE.name());
        delegateAction.setMaxOperatorCount(1);

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程").code("leave").createdOperator(user).form(leaveForm())
                .addNode(startNode).addNode(approvalNode).addNode(endNode).build();
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
        FlowActionRequest userAction = new FlowActionRequest();
        userAction.setFormData(data);
        userAction.setRecordId(userRecords.get(0).getId());
        userAction.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "提交", user.getUserId()));
        factory.flowService.action(userAction);

        List<FlowRecord> bossRecords = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecords.size());

        IFlowAction delegateAction2 = approvalNode.actionManager().getActionByType(ActionType.DELEGATE.name());
        FlowActionRequest delegateRequest = new FlowActionRequest();
        delegateRequest.setFormData(data);
        delegateRequest.setRecordId(bossRecords.get(0).getId());
        FlowAdviceBody delegateAdvice = new FlowAdviceBody(delegateAction2.id(), boss.getUserId());
        delegateAdvice.setForwardOperatorIds(List.of(target1.getUserId(), target2.getUserId()));
        delegateRequest.setAdvice(delegateAdvice);

        assertThrows(FlowExecutionException.class, () -> factory.flowService.action(delegateRequest));
    }

    /**
     * 加签 - 选择人数超过上限（maxOperatorCount=1）应报错
     */
    @Test
    void testAddAuditExceedMaxCount() {
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User target1 = new User(3, "target1");
        User target2 = new User(4, "target2");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(target1);
        factory.userGateway.save(target2);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder().addStrategy(permission()).build())
                .build();

        ApprovalNode approvalNode = ApprovalNode.builder()
                .name("审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(permission())
                        .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .build())
                .build();
        AddAuditAction addAuditAction = (AddAuditAction) approvalNode.actionManager().getActionByType(ActionType.ADD_AUDIT.name());
        addAuditAction.setMaxOperatorCount(1);

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程").code("leave").createdOperator(user).form(leaveForm())
                .addNode(startNode).addNode(approvalNode).addNode(endNode).build();
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
        FlowActionRequest userAction = new FlowActionRequest();
        userAction.setFormData(data);
        userAction.setRecordId(userRecords.get(0).getId());
        userAction.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "提交", user.getUserId()));
        factory.flowService.action(userAction);

        List<FlowRecord> bossRecords = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecords.size());

        IFlowAction addAuditAction2 = approvalNode.actionManager().getActionByType(ActionType.ADD_AUDIT.name());
        FlowActionRequest addAuditRequest = new FlowActionRequest();
        addAuditRequest.setFormData(data);
        addAuditRequest.setRecordId(bossRecords.get(0).getId());
        FlowAdviceBody addAuditAdvice = new FlowAdviceBody(addAuditAction2.id(), boss.getUserId());
        addAuditAdvice.setForwardOperatorIds(List.of(target1.getUserId(), target2.getUserId()));
        addAuditRequest.setAdvice(addAuditAdvice);

        assertThrows(FlowExecutionException.class, () -> factory.flowService.action(addAuditRequest));
    }

    /**
     * 转办 - 未配置 maxOperatorCount 时选择任意数量均不限制
     */
    @Test
    void testTransferDefaultNoLimit() {
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User target1 = new User(3, "target1");
        User target2 = new User(4, "target2");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(target1);
        factory.userGateway.save(target2);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder().addStrategy(permission()).build())
                .build();

        ApprovalNode approvalNode = ApprovalNode.builder()
                .name("审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(permission())
                        .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程").code("leave").createdOperator(user).form(leaveForm())
                .addNode(startNode).addNode(approvalNode).addNode(endNode).build();
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
        FlowActionRequest userAction = new FlowActionRequest();
        userAction.setFormData(data);
        userAction.setRecordId(userRecords.get(0).getId());
        userAction.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "提交", user.getUserId()));
        factory.flowService.action(userAction);

        List<FlowRecord> bossRecords = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecords.size());

        IFlowAction transferAction = approvalNode.actionManager().getActionByType(ActionType.TRANSFER.name());
        FlowActionRequest transferRequest = new FlowActionRequest();
        transferRequest.setFormData(data);
        transferRequest.setRecordId(bossRecords.get(0).getId());
        FlowAdviceBody transferAdvice = new FlowAdviceBody(transferAction.id(), boss.getUserId());
        transferAdvice.setForwardOperatorIds(List.of(target1.getUserId(), target2.getUserId()));
        transferRequest.setAdvice(transferAdvice);
        factory.flowService.action(transferRequest);

        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(target1.getUserId()).size());
        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(target2.getUserId()).size());
    }

    /**
     * 序列化兼容 - OperatorLoadStrategy 未配置 maxOperatorCount 时默认 -1（不限制）
     */
    @Test
    void testOperatorLoadStrategyBackwardCompatible() {
        // 旧数据没有 maxOperatorCount 字段
        Map<String, Object> oldMap = Map.of(
                "type", "OperatorLoadStrategy",
                "selectType", "INITIATOR_SELECT",
                "script", "def run(request){return [2,3]}"
        );
        OperatorLoadStrategy strategy = OperatorLoadStrategy.fromMap(oldMap);
        assertNotNull(strategy);
        assertEquals(OperatorSelectType.INITIATOR_SELECT, strategy.getSelectType());
        assertEquals(-1, strategy.getMaxOperatorCount());
    }

    /**
     * 序列化兼容 - OperatorLoadStrategy 配置了 maxOperatorCount 时往返一致
     */
    @Test
    void testOperatorLoadStrategyRoundTrip() {
        OperatorLoadStrategy strategy = OperatorLoadStrategy.initiatorSelectStrategy("def run(request){return [2,3]}", 2);
        Map<String, Object> map = strategy.toMap();
        assertEquals(2, map.get("maxOperatorCount"));

        OperatorLoadStrategy restored = OperatorLoadStrategy.fromMap(map);
        assertNotNull(restored);
        assertEquals(2, restored.getMaxOperatorCount());
    }

    /**
     * 序列化兼容 - 未配置 maxOperatorCount 时不写出该字段（保持旧数据格式）
     */
    @Test
    void testOperatorLoadStrategyDefaultNotSerialized() {
        OperatorLoadStrategy strategy = OperatorLoadStrategy.initiatorSelectStrategy("def run(request){return [2,3]}");
        Map<String, Object> map = strategy.toMap();
        assertFalse(map.containsKey("maxOperatorCount"));
    }

    /**
     * 序列化兼容 - TransferAction 未配置 maxOperatorCount 时默认 -1
     */
    @Test
    void testTransferActionBackwardCompatible() {
        Map<String, Object> oldMap = Map.of(
                "type", "TRANSFER",
                "id", "action-transfer-1",
                "script", "def run(request){return [2]}"
        );
        TransferAction action = TransferAction.fromMap(oldMap);
        assertNotNull(action);
        assertEquals(-1, action.getMaxOperatorCount());
    }

    /**
     * 序列化兼容 - TransferAction 配置了 maxOperatorCount 时往返一致
     */
    @Test
    void testTransferActionRoundTrip() {
        TransferAction action = TransferAction.defaultAction();
        action.setMaxOperatorCount(3);
        Map<String, Object> map = action.toMap();
        assertEquals(3, map.get("maxOperatorCount"));

        TransferAction restored = TransferAction.fromMap(map);
        assertNotNull(restored);
        assertEquals(3, restored.getMaxOperatorCount());
    }

    /**
     * 序列化兼容 - DelegateAction / AddAuditAction 配置了 maxOperatorCount 时往返一致
     */
    @Test
    void testOtherActionsRoundTrip() {
        DelegateAction delegateAction = DelegateAction.defaultAction();
        delegateAction.setMaxOperatorCount(2);
        DelegateAction delegateRestored = DelegateAction.fromMap(delegateAction.toMap());
        assertNotNull(delegateRestored);
        assertEquals(2, delegateRestored.getMaxOperatorCount());

        AddAuditAction addAuditAction = AddAuditAction.defaultAction();
        addAuditAction.setMaxOperatorCount(2);
        AddAuditAction addAuditRestored = AddAuditAction.fromMap(addAuditAction.toMap());
        assertNotNull(addAuditRestored);
        assertEquals(2, addAuditRestored.getMaxOperatorCount());
    }
}
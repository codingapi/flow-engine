package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.builder.ActionBuilder;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.edge.FlowEdge;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.form.FormMetaBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.gateway.impl.UserGateway;
import com.codingapi.flow.node.nodes.*;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowRevokeRequest;
import com.codingapi.flow.pojo.request.FlowUrgeRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.*;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.flow.script.runtime.IBeanFactory;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlowServiceTest {

    private final FlowRecordRepositoryImpl flowRecordRepository = new FlowRecordRepositoryImpl();
    private final UserGateway userGateway = new UserGateway();
    private final WorkflowBackupRepository workflowBackupRepository = new WorkflowBackupRepositoryImpl();
    private final WorkflowRepository workflowRepository = new WorkflowRepositoryImpl();
    private final ParallelBranchRepository parallelBranchRepository = new ParallelBranchRepositoryImpl();
    private final DelayTaskRepository delayTaskRepository = new DelayTaskRepositoryImpl();
    private final UrgeIntervalRepository urgeIntervalRepository = new UrgeIntervalRepositoryImpl();
    private final FlowService flowService = new FlowService(workflowRepository, userGateway, flowRecordRepository, workflowBackupRepository, parallelBranchRepository, delayTaskRepository,urgeIntervalRepository);

    @Test
    void create() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        userGateway.save(user);
        userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
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
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(2)]}"))
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
                .addEdge(new FlowEdge(startNode.getId(), bossNode.getId()))
                .addEdge(new FlowEdge(bossNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());

        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());
    }


    /**
     * 全部通过测试
     */
    @Test
    void pass() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        userGateway.save(user);
        userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
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
                        .addAction(new CustomAction())
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
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(2)]}"))
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
                .addEdge(new FlowEdge(startNode.getId(), bossNode.getId()))
                .addEdge(new FlowEdge(bossNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        List<FlowRecord> bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        flowService.action(bossRequest);

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 办理节点测试
     */
    @Test
    void handle() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        userGateway.save(user);
        userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
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
                        .addAction(new CustomAction())
                        .build())
                .build();

        HandleNode bossNode = HandleNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(2)]}"))
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
                .addEdge(new FlowEdge(startNode.getId(), bossNode.getId()))
                .addEdge(new FlowEdge(bossNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), null, user.getUserId()));
        flowService.action(userRequest);

        List<FlowRecord> bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        flowService.action(bossRequest);

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 抄送节点测试
     */
    @Test
    void notifyNode() {
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        userGateway.save(user);
        userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
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
                        .addAction(new CustomAction())
                        .build())
                .build();

        NotifyNode bossNode = NotifyNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(2)]}"))
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
                .addEdge(new FlowEdge(startNode.getId(), bossNode.getId()))
                .addEdge(new FlowEdge(bossNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), null, user.getUserId()));
        flowService.action(userRequest);

        List<FlowRecord> bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(0, bossRecordList.size());

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(userRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());
    }


    /**
     * 条件分支测试
     */
    @Test
    void condition() {

        User user = new User(1, "user");
        User depart = new User(2, "depart");
        User boss = new User(3, "boss");
        userGateway.save(user);
        userGateway.save(depart);
        userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
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
                .build();

        ConditionBranchNode departConditionNode = ConditionBranchNode.builder()
                .name("条件分支")
                .conditionScript("def run(request){return request.getFormData('days') <= 3}")
                .order(1)
                .build();

        ConditionBranchNode bossConditionNode = ConditionBranchNode.builder()
                .name("条件分支")
                .conditionScript("def run(request){return request.getFormData('days') > 3}")
                .order(2)
                .build();

        ApprovalNode departApprovalNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(2)]}"))
                        .build()
                )
                .build();

        ApprovalNode bossApprovalNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(3)]}"))
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
                .addNode(departConditionNode)
                .addNode(bossConditionNode)
                .addNode(departApprovalNode)
                .addNode(bossApprovalNode)
                .addNode(endNode)
                .addEdge(new FlowEdge(startNode.getId(), departConditionNode.getId()))
                .addEdge(new FlowEdge(startNode.getId(), bossConditionNode.getId()))
                .addEdge(new FlowEdge(departConditionNode.getId(), departApprovalNode.getId()))
                .addEdge(new FlowEdge(bossConditionNode.getId(), bossApprovalNode.getId()))
                .addEdge(new FlowEdge(departApprovalNode.getId(), endNode.getId()))
                .addEdge(new FlowEdge(bossApprovalNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 3, "reason", "leave");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());

        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        List<FlowRecord> departRecordList = flowRecordRepository.findTodoByOperator(depart.getUserId());
        assertEquals(1, departRecordList.size());


        List<IFlowAction> departActions = departApprovalNode.actionManager().getActions();

        FlowActionRequest departRequest = new FlowActionRequest();
        departRequest.setFormData(data);
        departRequest.setRecordId(departRecordList.get(0).getId());
        departRequest.setAdvice(new FlowAdviceBody(departActions.get(0).id(), "同意", depart.getUserId()));
        flowService.action(departRequest);

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(departRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 拒绝测试
     */
    @Test
    void reject() {

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);
        FlowScriptContext.getInstance().setBeanFactory(new IBeanFactory() {
            @Override
            public FlowRecord getRecordById(long id) {
                return flowRecordRepository.get(id);
            }
        });

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        userGateway.save(user);
        userGateway.save(boss);

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
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
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(2)]}"))
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
                .addEdge(new FlowEdge(startNode.getId(), bossNode.getId()))
                .addEdge(new FlowEdge(bossNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());

        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        List<FlowRecord> bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(1).id(), "不同意", boss.getUserId()));
        flowService.action(bossRequest);

        List<FlowRecord> userToDoList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userToDoList.size());

        userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userToDoList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        flowService.action(bossRequest);

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(5, records.size());
        assertEquals(5, records.stream().filter(FlowRecord::isFinish).toList().size());
    }


    /**
     * 并行分支测试
     */
    @Test
    void parallel() {

        User user = new User(1, "user");
        User depart = new User(2, "depart");
        User boss = new User(3, "boss");
        userGateway.save(user);
        userGateway.save(depart);
        userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
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
                .build();

        ParallelBranchNode parallelBranchNode1 = ParallelBranchNode.builder()
                .name("并行分支1")
                .order(1)
                .build();

        ParallelBranchNode parallelBranchNode2 = ParallelBranchNode.builder()
                .name("并行分支2")
                .order(2)
                .build();

        ApprovalNode departApprovalNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(2)]}"))
                        .build()
                )
                .build();

        ApprovalNode bossApprovalNode = ApprovalNode.builder()
                .name("老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(3)]}"))
                        .build()
                )
                .build();

        ApprovalNode bigBossApprovalNode = ApprovalNode.builder()
                .name("大老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(3)]}"))
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
                .addNode(parallelBranchNode1)
                .addNode(parallelBranchNode2)
                .addNode(departApprovalNode)
                .addNode(bossApprovalNode)
                .addNode(bigBossApprovalNode)
                .addNode(endNode)
                .addEdge(new FlowEdge(startNode.getId(), parallelBranchNode1.getId()))
                .addEdge(new FlowEdge(startNode.getId(), parallelBranchNode2.getId()))
                .addEdge(new FlowEdge(parallelBranchNode1.getId(), departApprovalNode.getId()))
                .addEdge(new FlowEdge(parallelBranchNode2.getId(), bossApprovalNode.getId()))
                .addEdge(new FlowEdge(bossApprovalNode.getId(), bigBossApprovalNode.getId()))
                .addEdge(new FlowEdge(departApprovalNode.getId(), endNode.getId()))
                .addEdge(new FlowEdge(bigBossApprovalNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 3, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());

        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        List<FlowRecord> departRecordList = flowRecordRepository.findTodoByOperator(depart.getUserId());
        assertEquals(1, departRecordList.size());

        List<FlowRecord> boosRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());

        List<IFlowAction> departActions = departApprovalNode.actionManager().getActions();

        FlowActionRequest departRequest = new FlowActionRequest();
        departRequest.setFormData(data);
        departRequest.setRecordId(departRecordList.get(0).getId());
        departRequest.setAdvice(new FlowAdviceBody(departActions.get(0).id(), "同意", depart.getUserId()));
        flowService.action(departRequest);

        boosRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());

        List<IFlowAction> bossActions = bossApprovalNode.actionManager().getActions();

        FlowActionRequest dossRequest = new FlowActionRequest();
        dossRequest.setFormData(data);
        dossRequest.setRecordId(boosRecordList.get(0).getId());
        dossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        flowService.action(dossRequest);


        boosRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());

        List<IFlowAction> bigBossActions = bigBossApprovalNode.actionManager().getActions();

        FlowActionRequest bigBossRequest = new FlowActionRequest();
        bigBossRequest.setFormData(data);
        bigBossRequest.setRecordId(boosRecordList.get(0).getId());
        bigBossRequest.setAdvice(new FlowAdviceBody(bigBossActions.get(0).id(), "同意", boss.getUserId()));
        flowService.action(bigBossRequest);

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(departRecordList.get(0).getProcessId());
        assertEquals(5, records.size());
        assertEquals(0, records.stream().filter(FlowRecord::isTodo).toList().size());
        assertEquals(5, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 包容分支测试
     */
    @Test
    void inclusive() {

        User user = new User(1, "user");
        User depart = new User(2, "depart");
        User boss = new User(3, "boss");
        userGateway.save(user);
        userGateway.save(depart);
        userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
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
                .build();

        InclusiveBranchNode parallelBranchNode1 = InclusiveBranchNode.builder()
                .name("包容分支1")
                .conditionScript("def run(request){return true}")
                .order(1)
                .build();

        InclusiveBranchNode parallelBranchNode2 = InclusiveBranchNode.builder()
                .name("包容分支2")
                .conditionScript("def run(request){return request.getFormData('days') >= 3}")
                .order(2)
                .build();

        ApprovalNode departApprovalNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(2)]}"))
                        .build()
                )
                .build();

        ApprovalNode bossApprovalNode = ApprovalNode.builder()
                .name("老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(3)]}"))
                        .build()
                )
                .build();

        ApprovalNode bigBossApprovalNode = ApprovalNode.builder()
                .name("大老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(3)]}"))
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
                .addNode(parallelBranchNode1)
                .addNode(parallelBranchNode2)
                .addNode(departApprovalNode)
                .addNode(bossApprovalNode)
                .addNode(bigBossApprovalNode)
                .addNode(endNode)
                .addEdge(new FlowEdge(startNode.getId(), parallelBranchNode1.getId()))
                .addEdge(new FlowEdge(startNode.getId(), parallelBranchNode2.getId()))
                .addEdge(new FlowEdge(parallelBranchNode1.getId(), departApprovalNode.getId()))
                .addEdge(new FlowEdge(parallelBranchNode2.getId(), bossApprovalNode.getId()))
                .addEdge(new FlowEdge(bossApprovalNode.getId(), bigBossApprovalNode.getId()))
                .addEdge(new FlowEdge(departApprovalNode.getId(), endNode.getId()))
                .addEdge(new FlowEdge(bigBossApprovalNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 3, "reason", "leave");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);

        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());

        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        List<FlowRecord> departRecordList = flowRecordRepository.findTodoByOperator(depart.getUserId());
        assertEquals(1, departRecordList.size());

        List<FlowRecord> boosRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());


        List<IFlowAction> departActions = departApprovalNode.actionManager().getActions();

        FlowActionRequest departRequest = new FlowActionRequest();
        departRequest.setFormData(data);
        departRequest.setRecordId(departRecordList.get(0).getId());
        departRequest.setAdvice(new FlowAdviceBody(departActions.get(0).id(), "同意", depart.getUserId()));
        flowService.action(departRequest);

        boosRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());

        List<IFlowAction> bossActions = bossApprovalNode.actionManager().getActions();

        FlowActionRequest dossRequest = new FlowActionRequest();
        dossRequest.setFormData(data);
        dossRequest.setRecordId(boosRecordList.get(0).getId());
        dossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        flowService.action(dossRequest);


        boosRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());

        List<IFlowAction> bigBossActions = bigBossApprovalNode.actionManager().getActions();

        FlowActionRequest bigBossRequest = new FlowActionRequest();
        bigBossRequest.setFormData(data);
        bigBossRequest.setRecordId(boosRecordList.get(0).getId());
        bigBossRequest.setAdvice(new FlowAdviceBody(bigBossActions.get(0).id(), "同意", boss.getUserId()));
        flowService.action(bigBossRequest);

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(departRecordList.get(0).getProcessId());
        assertEquals(5, records.size());
        assertEquals(0, records.stream().filter(FlowRecord::isTodo).toList().size());
        assertEquals(5, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 路由节点测试
     */
    @Test
    void router() {

        User user = new User(1, "user");
        User depart = new User(2, "depart");
        User boss = new User(3, "boss");

        userGateway.save(user);
        userGateway.save(depart);
        userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
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
                        .addAction(new CustomAction())
                        .build())
                .build();

        ConditionBranchNode departConditionNode = ConditionBranchNode.builder()
                .name("条件分支")
                .conditionScript("def run(request){return request.getFormData('days') <= 3}")
                .order(1)
                .build();

        ConditionBranchNode bossConditionNode = ConditionBranchNode.builder()
                .name("条件分支")
                .conditionScript("def run(request){return request.getFormData('days') > 3}")
                .order(2)
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(3)]}"))
                        .build()
                )
                .build();

        RouterNode routerNode = RouterNode.builder()
                .name("路由节点")
                .routerNodeScript(String.format("def run(request){return '%s'}", bossNode.getId()))
                .build();

        ApprovalNode departNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(2)]}"))
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
                .addNode(departConditionNode)
                .addNode(bossConditionNode)
                .addNode(departNode)
                .addNode(bossNode)
                .addNode(routerNode)
                .addNode(endNode)
                .addEdge(new FlowEdge(startNode.getId(), bossConditionNode.getId()))
                .addEdge(new FlowEdge(bossConditionNode.getId(), bossNode.getId()))
                .addEdge(new FlowEdge(bossNode.getId(), endNode.getId()))

                .addEdge(new FlowEdge(startNode.getId(), departConditionNode.getId()))
                .addEdge(new FlowEdge(departConditionNode.getId(), departNode.getId()))
                .addEdge(new FlowEdge(departNode.getId(), routerNode.getId()))
                .addEdge(new FlowEdge(routerNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 2, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);

        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());

        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest submitRequest = new FlowActionRequest();
        submitRequest.setFormData(data);
        submitRequest.setRecordId(userRecordList.get(0).getId());
        submitRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        flowService.action(submitRequest);

        List<FlowRecord> departRecordList = flowRecordRepository.findTodoByOperator(depart.getUserId());
        assertEquals(1, departRecordList.size());


        List<IFlowAction> departActions = departNode.actionManager().getActions();

        FlowActionRequest departRequest = new FlowActionRequest();
        departRequest.setFormData(data);
        departRequest.setRecordId(departRecordList.get(0).getId());
        departRequest.setAdvice(new FlowAdviceBody(departActions.get(0).id(), "同意", depart.getUserId()));
        flowService.action(departRequest);


        List<FlowRecord> bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        flowService.action(bossRequest);

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(userRecordList.get(0).getProcessId());
        assertEquals(4, records.size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 全部通过测试
     */
    @Test
    void delay() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        userGateway.save(user);
        userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
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
                        .addAction(new CustomAction())
                        .build())
                .build();

        DelayNode delayNode = DelayNode.builder()
                .name("延迟节点")
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(2)]}"))
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
                .addNode(delayNode)
                .addNode(bossNode)
                .addNode(endNode)
                .addEdge(new FlowEdge(startNode.getId(), delayNode.getId()))
                .addEdge(new FlowEdge(delayNode.getId(), bossNode.getId()))
                .addEdge(new FlowEdge(bossNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());

        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        List<FlowRecord> bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(0, bossRecordList.size());
        try {
            // 默认等待时间为5秒
            Thread.sleep(8000);
        } catch (Exception ignore) {
        }

        bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        flowService.action(bossRequest);

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 触发节点测试
     */
    @Test
    void trigger() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        userGateway.save(user);
        userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
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
                        .addAction(new CustomAction())
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
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(2)]}"))
                        .build()
                )
                .build();

        TriggerNode triggerNode = TriggerNode.builder()
                .name("触发流程")
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(triggerNode)
                .addNode(endNode)
                .addEdge(new FlowEdge(startNode.getId(), triggerNode.getId()))
                .addEdge(new FlowEdge(triggerNode.getId(), bossNode.getId()))
                .addEdge(new FlowEdge(bossNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        List<FlowRecord> bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        flowService.action(bossRequest);

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 子流程节点测试
     */
    @Test
    void subProcess() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        userGateway.save(user);
        userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
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
                        .addAction(new CustomAction())
                        .build())
                .build();

        SubProcessNode subProcessNode = SubProcessNode.builder()
                .name("子流程")
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(2)]}"))
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
                .addNode(subProcessNode)
                .addNode(endNode)
                .addEdge(new FlowEdge(startNode.getId(), bossNode.getId()))
                .addEdge(new FlowEdge(bossNode.getId(), subProcessNode.getId()))
                .addEdge(new FlowEdge(subProcessNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = userCreateRequest.toActionRequest(userRecordList.get(0).getId());
        flowService.action(userRequest);

        List<FlowRecord> bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        flowService.action(bossRequest);

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());

        // 为老板再次创建一个待办流程
        bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

    }


    /**
     * 保存测试
     */
    @Test
    void save() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        userGateway.save(user);
        userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
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
                        .addAction(new CustomAction())
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
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(2)]}"))
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
                .addEdge(new FlowEdge(startNode.getId(), bossNode.getId()))
                .addEdge(new FlowEdge(bossNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        Map<String, Object> data = new HashMap<>(Map.of("name", "lorne", "days", 1, "reason", "leave"));

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        // 保存数据
        data.put("reason", "test");
        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(1).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        Map<String, Object> currentFormData = userRecordList.get(0).getFormData();
        assertEquals("test", currentFormData.get("reason"));

        userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        List<FlowRecord> bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        flowService.action(bossRequest);

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 加签测试
     */
    @Test
    void addAudit() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User lorne = new User(3, "lorne");
        userGateway.save(user);
        userGateway.save(boss);
        userGateway.save(lorne);

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
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
                        .addAction(new CustomAction())
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
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(2)]}"))
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
                .addEdge(new FlowEdge(startNode.getId(), bossNode.getId()))
                .addEdge(new FlowEdge(bossNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        Map<String, Object> data = new HashMap<>(Map.of("name", "lorne", "days", 1, "reason", "leave"));

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        List<FlowRecord> bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest addAuditRequest = new FlowActionRequest();
        addAuditRequest.setFormData(data);
        addAuditRequest.setRecordId(bossRecordList.get(0).getId());

        FlowAdviceBody addAuditAdviceBody = new FlowAdviceBody(bossActions.get(3).id(), boss.getUserId());
        addAuditAdviceBody.setForwardOperatorIds(List.of(3L));
        addAuditRequest.setAdvice(addAuditAdviceBody);
        flowService.action(addAuditRequest);

        bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        List<FlowRecord> lorneRecordList = flowRecordRepository.findTodoByOperator(lorne.getUserId());
        assertEquals(0, lorneRecordList.size());

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        flowService.action(bossRequest);

        lorneRecordList = flowRecordRepository.findTodoByOperator(lorne.getUserId());
        assertEquals(1, lorneRecordList.size());

        FlowActionRequest lorneRequest = new FlowActionRequest();
        lorneRequest.setFormData(data);
        lorneRequest.setRecordId(lorneRecordList.get(0).getId());
        lorneRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", lorne.getUserId()));
        flowService.action(lorneRequest);

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(4, records.size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 转办测试
     */
    @Test
    void transfer() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User lorne = new User(3, "lorne");
        userGateway.save(user);
        userGateway.save(boss);
        userGateway.save(lorne);

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
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
                        .addAction(new CustomAction())
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
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(2)]}"))
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
                .addEdge(new FlowEdge(startNode.getId(), bossNode.getId()))
                .addEdge(new FlowEdge(bossNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        Map<String, Object> data = new HashMap<>(Map.of("name", "lorne", "days", 1, "reason", "leave"));

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        List<FlowRecord> bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest transferRequest = new FlowActionRequest();
        transferRequest.setFormData(data);
        transferRequest.setRecordId(bossRecordList.get(0).getId());

        FlowAdviceBody transferAdviceBody = new FlowAdviceBody(bossActions.get(4).id(), boss.getUserId());
        transferAdviceBody.setForwardOperatorIds(List.of(3L));
        transferRequest.setAdvice(transferAdviceBody);
        flowService.action(transferRequest);

        bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(0, bossRecordList.size());

        List<FlowRecord> lorneRecordList = flowRecordRepository.findTodoByOperator(lorne.getUserId());
        assertEquals(1, lorneRecordList.size());

        FlowActionRequest lorneRequest = new FlowActionRequest();
        lorneRequest.setFormData(data);
        lorneRequest.setRecordId(lorneRecordList.get(0).getId());
        lorneRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", lorne.getUserId()));
        flowService.action(lorneRequest);

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(lorneRecordList.get(0).getProcessId());
        assertEquals(4, records.size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 退回节点测试
     */
    @Test
    void returnNode() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");

        userGateway.save(user);
        userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
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
                        .addAction(new CustomAction())
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
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(2)]}"))
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
                .addEdge(new FlowEdge(startNode.getId(), bossNode.getId()))
                .addEdge(new FlowEdge(bossNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        Map<String, Object> data = new HashMap<>(Map.of("name", "lorne", "days", 1, "reason", "leave"));

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        List<FlowRecord> bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest returnRequest = new FlowActionRequest();
        returnRequest.setFormData(data);
        returnRequest.setRecordId(bossRecordList.get(0).getId());

        FlowAdviceBody backAdviceBody = new FlowAdviceBody(bossActions.get(5).id(), boss.getUserId());
        backAdviceBody.setBackNodeId(startNode.getId());
        returnRequest.setAdvice(backAdviceBody);
        flowService.action(returnRequest);

        userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), boss.getUserId()));
        flowService.action(bossRequest);

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(5, records.size());
        assertEquals(5, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 委托测试
     */
    @Test
    void delegate() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User lorne = new User(3, "lorne");

        userGateway.save(user);
        userGateway.save(boss);
        userGateway.save(lorne);

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
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
                        .addAction(new CustomAction())
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
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(2)]}"))
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
                .addEdge(new FlowEdge(startNode.getId(), bossNode.getId()))
                .addEdge(new FlowEdge(bossNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        Map<String, Object> data = new HashMap<>(Map.of("name", "lorne", "days", 1, "reason", "leave"));

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        List<FlowRecord> bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest delegateRequest = new FlowActionRequest();
        delegateRequest.setFormData(data);
        delegateRequest.setRecordId(bossRecordList.get(0).getId());

        FlowAdviceBody delegateAdviceBody = new FlowAdviceBody(bossActions.get(6).id(), boss.getUserId());
        delegateAdviceBody.setForwardOperatorIds(List.of(lorne.getUserId()));
        delegateRequest.setAdvice(delegateAdviceBody);
        flowService.action(delegateRequest);

        List<FlowRecord> lorneRecordList = flowRecordRepository.findTodoByOperator(lorne.getUserId());
        assertEquals(1, lorneRecordList.size());

        FlowActionRequest lorneRequest = new FlowActionRequest();
        lorneRequest.setFormData(data);
        lorneRequest.setRecordId(lorneRecordList.get(0).getId());
        lorneRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", lorne.getUserId()));
        flowService.action(lorneRequest);

        bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), boss.getUserId()));
        flowService.action(bossRequest);

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(5, records.size());
        assertEquals(5, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 自定义事件测试
     */
    @Test
    void custom() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");

        userGateway.save(user);
        userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
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
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(2)]}"))
                        .build()
                )
                .actions(ActionBuilder.builder()
                        .addAction(new CustomAction())
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
                .addEdge(new FlowEdge(startNode.getId(), bossNode.getId()))
                .addEdge(new FlowEdge(bossNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        Map<String, Object> data = new HashMap<>(Map.of("name", "lorne", "days", 1, "reason", "leave"));

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        List<FlowRecord> bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(7).id(), boss.getUserId()));
        flowService.action(bossRequest);

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());

    }



    /**
     * 撤回测试
     */
    @Test
    void revoke() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        userGateway.save(user);
        userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
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
                        .addAction(new CustomAction())
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
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(2)]}"))
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
                .addEdge(new FlowEdge(startNode.getId(), bossNode.getId()))
                .addEdge(new FlowEdge(bossNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        List<FlowRecord> bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        List<FlowRecord> userDoneList = flowRecordRepository.findDoneByOperator(user.getUserId());
        assertEquals(1, userDoneList.size());

        flowService.revoke(new FlowRevokeRequest(userDoneList.get(0).getId(), user.getUserId()));

        userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(0, bossRecordList.size());

        userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        flowService.action(bossRequest);

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(4, records.size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 催办测试
     */
    @Test
    void urge() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        userGateway.save(user);
        userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
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
                        .addAction(new CustomAction())
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
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(2)]}"))
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
                .addEdge(new FlowEdge(startNode.getId(), bossNode.getId()))
                .addEdge(new FlowEdge(bossNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        List<FlowRecord> bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        List<FlowRecord> userDoneList = flowRecordRepository.findDoneByOperator(user.getUserId());
        assertEquals(1, userDoneList.size());

        flowService.urge(new FlowUrgeRequest(userDoneList.get(0).getId(), user.getUserId()));

        bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        flowService.action(bossRequest);

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());

    }



    /**
     * 流程干预测试
     */
    @Test
    void interfere() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User lorne = new User(3, "lorne",true);

        userGateway.save(user);
        userGateway.save(boss);
        userGateway.save(lorne);

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
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
                        .addAction(new CustomAction())
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
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(2)]}"))
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
                .addEdge(new FlowEdge(startNode.getId(), bossNode.getId()))
                .addEdge(new FlowEdge(bossNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        List<FlowRecord> bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", lorne.getUserId()));
        flowService.action(bossRequest);

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());

    }

}
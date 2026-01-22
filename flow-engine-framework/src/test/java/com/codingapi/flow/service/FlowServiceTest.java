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
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.*;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.flow.script.runtime.IBeanFactory;
import com.codingapi.flow.strategy.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.OperatorLoadStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

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
    private final FlowService flowService = new FlowService(workflowRepository, userGateway, flowRecordRepository, workflowBackupRepository, parallelBranchRepository,delayTaskRepository);

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

        FlowCreateRequest userRequest = new FlowCreateRequest();
        userRequest.setWorkId(workflow.getId());
        userRequest.setFormData(data);
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));

        flowService.create(userRequest);

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
        userCreateRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
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

        List<FlowRecord> records = flowRecordRepository.findRecordsByProcessId(bossRecordList.get(0).getProcessId());
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

        Map<String,Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), null, user.getUserId()));
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

        List<FlowRecord> records = flowRecordRepository.findRecordsByProcessId(bossRecordList.get(0).getProcessId());
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

        Map<String,Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), null, user.getUserId()));
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

        List<FlowRecord> records = flowRecordRepository.findRecordsByProcessId(userRecordList.get(0).getProcessId());
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

        BranchNodeBranchNode departConditionNode = BranchNodeBranchNode.builder()
                .name("条件分支")
                .conditionScript("def run(request){return request.getFormData('days') <= 3}")
                .order(1)
                .build();

        BranchNodeBranchNode bossConditionNode = BranchNodeBranchNode.builder()
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
        userCreateRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));

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

        List<FlowRecord> records = flowRecordRepository.findRecordsByProcessId(departRecordList.get(0).getProcessId());
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
        userCreateRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));

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

        List<FlowRecord> records = flowRecordRepository.findRecordsByProcessId(bossRecordList.get(0).getProcessId());
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
        userCreateRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));

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

        List<FlowRecord> records = flowRecordRepository.findRecordsByProcessId(departRecordList.get(0).getProcessId());
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

        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkId(workflow.getId());
        request.setFormData(data);
        List<IFlowAction> startActions = startNode.actionManager().getActions();
        request.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));

        flowService.create(request);

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

        List<FlowRecord> records = flowRecordRepository.findRecordsByProcessId(departRecordList.get(0).getProcessId());
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

        BranchNodeBranchNode departConditionNode = BranchNodeBranchNode.builder()
                .name("条件分支")
                .conditionScript("def run(request){return request.getFormData('days') <= 3}")
                .order(1)
                .build();

        BranchNodeBranchNode bossConditionNode = BranchNodeBranchNode.builder()
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

        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkId(workflow.getId());
        request.setFormData(data);
        List<IFlowAction> userActions = startNode.actionManager().getActions();
        request.setAdvice(new FlowAdviceBody(userActions.get(0).id(), "同意", user.getUserId()));

        flowService.create(request);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest submitRequest = new FlowActionRequest();
        submitRequest.setFormData(data);
        submitRequest.setRecordId(userRecordList.get(0).getId());
        submitRequest.setAdvice(new FlowAdviceBody(userActions.get(0).id(), "同意", user.getUserId()));
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

        List<FlowRecord> records = flowRecordRepository.findRecordsByProcessId(userRecordList.get(0).getProcessId());
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

        List<IFlowAction> actions = startNode.actionManager().getActions();
        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setAdvice(new FlowAdviceBody(actions.get(0).id(), "同意", user.getUserId()));

        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(actions.get(0).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        List<FlowRecord> bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(0, bossRecordList.size());
        try {
            // 默认等待时间为5秒
            Thread.sleep(8000);
        }catch (Exception ignore){}

        bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        flowService.action(bossRequest);

        List<FlowRecord> records = flowRecordRepository.findRecordsByProcessId(bossRecordList.get(0).getProcessId());
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

        TriggerNode triggerNode = TriggerNode.builder()
                .name("触发流程")
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
        userCreateRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
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

        List<FlowRecord> records = flowRecordRepository.findRecordsByProcessId(bossRecordList.get(0).getProcessId());
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
                .addEdge(new FlowEdge(startNode.getId(), subProcessNode.getId()))
                .addEdge(new FlowEdge(subProcessNode.getId(), bossNode.getId()))
                .addEdge(new FlowEdge(bossNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
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

        List<FlowRecord> records = flowRecordRepository.findRecordsByProcessId(bossRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());

    }



}
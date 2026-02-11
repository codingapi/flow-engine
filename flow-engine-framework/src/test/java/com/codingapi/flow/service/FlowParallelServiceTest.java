package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.form.FormMetaBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.gateway.impl.UserGateway;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.*;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.*;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlowParallelServiceTest {

    private final FlowRecordRepositoryImpl flowRecordRepository = new FlowRecordRepositoryImpl();
    private final UserGateway userGateway = new UserGateway();
    private final WorkflowBackupRepository workflowBackupRepository = new WorkflowBackupRepositoryImpl();
    private final WorkflowRepository workflowRepository = new WorkflowRepositoryImpl();
    private final ParallelBranchRepository parallelBranchRepository = new ParallelBranchRepositoryImpl();
    private final DelayTaskRepository delayTaskRepository = new DelayTaskRepositoryImpl();
    private final UrgeIntervalRepository urgeIntervalRepository = new UrgeIntervalRepositoryImpl();
    private final FlowService flowService = new FlowService(workflowRepository, userGateway, flowRecordRepository, workflowBackupRepository, parallelBranchRepository, delayTaskRepository, urgeIntervalRepository);


    /**
     * 并行分支测试
     */
    @Test
    void parallelAndParallel() {

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


        ApprovalNode departApprovalNode1 = ApprovalNode.builder()
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

        ApprovalNode bossApprovalNode1 = ApprovalNode.builder()
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

        ApprovalNode bigBossApprovalNode1 = ApprovalNode.builder()
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


        ParallelBranchNode parallelBranchNode11 = ParallelBranchNode.builder()
                .name("并行分支1")
                .blocks(departApprovalNode1)
                .order(1)
                .build();

        ParallelBranchNode parallelBranchNode12 = ParallelBranchNode.builder()
                .name("并行分支2")
                .blocks(bossApprovalNode1, bigBossApprovalNode1)
                .order(2)
                .build();

        ParallelNode parallelNode1 = ParallelNode.builder()
                .name("并行控制节点")
                .blocks(parallelBranchNode11, parallelBranchNode12)
                .build();


        ApprovalNode departApprovalNode2 = ApprovalNode.builder()
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

        ApprovalNode bossApprovalNode2 = ApprovalNode.builder()
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

        ApprovalNode bigBossApprovalNode2 = ApprovalNode.builder()
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


        ParallelBranchNode parallelBranchNode21 = ParallelBranchNode.builder()
                .name("并行分支1")
                .blocks(departApprovalNode2)
                .order(1)
                .build();

        ParallelBranchNode parallelBranchNode22 = ParallelBranchNode.builder()
                .name("并行分支2")
                .blocks(bossApprovalNode2, bigBossApprovalNode2)
                .order(2)
                .build();

        ParallelNode parallelNode2 = ParallelNode.builder()
                .name("并行控制节点")
                .blocks(parallelBranchNode21, parallelBranchNode22)
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(parallelNode1)
                .addNode(parallelNode2)
                .addNode(endNode)
                .build();

        workflowRepository.save(workflow);


        List<IFlowNode> nextNodes = workflow.nextNodes(bossApprovalNode1);
        assertEquals(1, nextNodes.size());
        assertEquals(bigBossApprovalNode1, nextNodes.get(0));


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

        List<IFlowAction> departActions = departApprovalNode1.actionManager().getActions();

        FlowActionRequest departRequest = new FlowActionRequest();
        departRequest.setFormData(data);
        departRequest.setRecordId(departRecordList.get(0).getId());
        departRequest.setAdvice(new FlowAdviceBody(departActions.get(0).id(), "同意", depart.getUserId()));
        flowService.action(departRequest);

        boosRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());

        List<IFlowAction> bossActions = bossApprovalNode1.actionManager().getActions();

        FlowActionRequest dossRequest = new FlowActionRequest();
        dossRequest.setFormData(data);
        dossRequest.setRecordId(boosRecordList.get(0).getId());
        dossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        flowService.action(dossRequest);


        boosRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());

        List<IFlowAction> bigBossActions = bigBossApprovalNode1.actionManager().getActions();

        FlowActionRequest bigBossRequest = new FlowActionRequest();
        bigBossRequest.setFormData(data);
        bigBossRequest.setRecordId(boosRecordList.get(0).getId());
        bigBossRequest.setAdvice(new FlowAdviceBody(bigBossActions.get(0).id(), "同意", boss.getUserId()));
        flowService.action(bigBossRequest);

        departRecordList = flowRecordRepository.findTodoByOperator(depart.getUserId());
        assertEquals(1, departRecordList.size());

        boosRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());


        departActions = departApprovalNode2.actionManager().getActions();

        departRequest = new FlowActionRequest();
        departRequest.setFormData(data);
        departRequest.setRecordId(departRecordList.get(0).getId());
        departRequest.setAdvice(new FlowAdviceBody(departActions.get(0).id(), "同意", depart.getUserId()));
        flowService.action(departRequest);

        boosRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());


        bossActions = bossApprovalNode2.actionManager().getActions();

        dossRequest = new FlowActionRequest();
        dossRequest.setFormData(data);
        dossRequest.setRecordId(boosRecordList.get(0).getId());
        dossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        flowService.action(dossRequest);


        boosRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());

        bigBossActions = bigBossApprovalNode2.actionManager().getActions();

        bigBossRequest = new FlowActionRequest();
        bigBossRequest.setFormData(data);
        bigBossRequest.setRecordId(boosRecordList.get(0).getId());
        bigBossRequest.setAdvice(new FlowAdviceBody(bigBossActions.get(0).id(), "同意", boss.getUserId()));
        flowService.action(bigBossRequest);


        List<FlowRecord> records = flowRecordRepository.findProcessRecords(departRecordList.get(0).getProcessId());
        assertEquals(8, records.size());
        assertEquals(0, records.stream().filter(FlowRecord::isTodo).toList().size());
        assertEquals(8, records.stream().filter(FlowRecord::isFinish).toList().size());

    }
}
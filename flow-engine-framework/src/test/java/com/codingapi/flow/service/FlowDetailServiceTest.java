package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.builder.ActionBuilder;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.form.FormMetaBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.gateway.impl.UserGateway;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowDetailRequest;
import com.codingapi.flow.pojo.request.FlowProcessNodeRequest;
import com.codingapi.flow.pojo.response.FlowContent;
import com.codingapi.flow.pojo.response.ProcessNode;
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
import static org.junit.jupiter.api.Assertions.assertNull;

public class FlowDetailServiceTest {

    private final FlowTodoRecordRepositoryImpl flowTodoRecordRepository = new FlowTodoRecordRepositoryImpl();
    private final FlowTodoMergeRepositoryImpl flowTodoMergeRepository = new FlowTodoMergeRepositoryImpl();
    private final FlowRecordRepositoryImpl flowRecordRepository = new FlowRecordRepositoryImpl();
    private final UserGateway userGateway = new UserGateway();
    private final WorkflowBackupRepository workflowBackupRepository = new WorkflowBackupRepositoryImpl();
    private final WorkflowRepository workflowRepository = new WorkflowRepositoryImpl();
    private final ParallelBranchRepository parallelBranchRepository = new ParallelBranchRepositoryImpl();
    private final DelayTaskRepository delayTaskRepository = new DelayTaskRepositoryImpl();
    private final UrgeIntervalRepository urgeIntervalRepository = new UrgeIntervalRepositoryImpl();
    private final FlowService flowService = new FlowService(workflowRepository, userGateway, flowRecordRepository, flowTodoRecordRepository, flowTodoMergeRepository, workflowBackupRepository, parallelBranchRepository, delayTaskRepository, urgeIntervalRepository);


    /**
     * 流程详情
     */
    @Test
    void detail() {

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
                .build();

        workflowRepository.save(workflow);

        FlowContent detail = flowService.detail(new FlowDetailRequest(workflow.getId(), user.getUserId()));
        assertEquals(detail.getForm().getCode(), form.getCode());
        assertEquals(detail.getActions().size(), startNode.actionManager().getActions().size());
        assertNull(detail.getTodos());
        assertNull(detail.getHistories());


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

        detail = flowService.detail(new FlowDetailRequest(userRecordList.get(0).getId(), user.getUserId()));
        assertEquals(detail.getForm().getCode(), form.getCode());
        assertEquals(detail.getActions().size(), startNode.actionManager().getActions().size());
        assertEquals(1, detail.getTodos().size());
        assertEquals(0, detail.getHistories().size());
        assertEquals(data, detail.getTodos().get(0).getData());


        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        List<FlowRecord> bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        detail = flowService.detail(new FlowDetailRequest(bossRecordList.get(0).getId(),boss.getUserId()));
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
        flowService.action(bossRequest);

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
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
                .build();

        workflowRepository.save(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<ProcessNode> nodeList = flowService.processNodes(new FlowProcessNodeRequest(workflow.getId(), user.getUserId(),data));
        assertEquals(3,nodeList.size());
        assertEquals(0,nodeList.stream().filter(ProcessNode::isHistory).toList().size());

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        nodeList = flowService.processNodes(new FlowProcessNodeRequest(userRecordList.get(0).getId(), user.getUserId(),data));
        assertEquals(3,nodeList.size());
        assertEquals(0,nodeList.stream().filter(ProcessNode::isHistory).toList().size());


        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        flowService.action(userRequest);

        List<FlowRecord> bossRecordList = flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        nodeList = flowService.processNodes(new FlowProcessNodeRequest(bossRecordList.get(0).getId(), boss.getUserId(),data));
        assertEquals(3,nodeList.size());
        assertEquals(1,nodeList.stream().filter(ProcessNode::isHistory).toList().size());

        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        flowService.action(bossRequest);

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }
}

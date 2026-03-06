package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.builder.ActionBuilder;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.gateway.impl.UserGateway;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FlowAddAuditServiceTest {


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
     * 加签测试
     */
    @Test
    void addAudit() {

        User user = new User(1, "user");
        User depart = new User(2, "depart");
        User boss = new User(3, "boss");

        userGateway.save(user);
        userGateway.save(boss);
        userGateway.save(depart);

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.NUMBER)
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
                        .addAction(new CustomAction())
                        .build())
                .build();

        ApprovalNode departNode = ApprovalNode.builder()
                .name("部门审批")
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

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(departNode)
                .addNode(bossNode)
                .addNode(endNode)
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

        List<FlowRecord> departRecordList = flowRecordRepository.findTodoByOperator(depart.getUserId());
        assertEquals(1, departRecordList.size());


        List<IFlowAction> departActions = departNode.actionManager().getActions();

        FlowActionRequest addAuditRequest = new FlowActionRequest();
        addAuditRequest.setFormData(data);
        addAuditRequest.setRecordId(departRecordList.get(0).getId());

        FlowAdviceBody addAuditAdviceBody = new FlowAdviceBody(departActions.get(3).id(), depart.getUserId());
        addAuditAdviceBody.setForwardOperatorIds(List.of(depart.getUserId()));
        addAuditRequest.setAdvice(addAuditAdviceBody);
        flowService.action(addAuditRequest);

        departRecordList = flowRecordRepository.findTodoByOperator(depart.getUserId());
        assertEquals(1, departRecordList.size());

        FlowActionRequest departRequest = new FlowActionRequest();
        departRequest.setFormData(data);
        departRequest.setRecordId(departRecordList.get(0).getId());
        departRequest.setAdvice(new FlowAdviceBody(departActions.get(0).id(), "同意", depart.getUserId()));
        flowService.action(departRequest);

        departRecordList = flowRecordRepository.findTodoByOperator(depart.getUserId());
        assertEquals(1, departRecordList.size());

        departRequest = new FlowActionRequest();
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

        List<FlowRecord> records = flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(4, records.size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).toList().size());

    }
}

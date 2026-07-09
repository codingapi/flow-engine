package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.ActionType;
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
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FlowAddAuditServiceTest {


    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();



    /**
     * 加签测试
     */
    @Test
    void addAudit() {

        User user = new User(1, "user");
        User depart = new User(2, "depart");
        User boss = new User(3, "boss");

        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(depart);

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

        ApprovalNode departNode = ApprovalNode.builder()
                .name("部门审批")
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

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [3]}").getKey()))
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

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = new HashMap<>(Map.of("name", "lorne", "days", 1, "reason", "leave"));

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkCode(workflow.getCode());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> departRecordList = factory.flowRecordRepository.findTodoByOperator(depart.getUserId());
        assertEquals(1, departRecordList.size());


        List<IFlowAction> departActions = departNode.actionManager().getActions();

        FlowActionRequest addAuditRequest = new FlowActionRequest();
        addAuditRequest.setFormData(data);
        addAuditRequest.setRecordId(departRecordList.get(0).getId());

        FlowAdviceBody addAuditAdviceBody = new FlowAdviceBody(departActions.get(3).id(), depart.getUserId());
        addAuditAdviceBody.setForwardOperatorIds(List.of(depart.getUserId()));
        addAuditRequest.setAdvice(addAuditAdviceBody);
        factory.flowService.action(addAuditRequest);

        departRecordList = factory.flowRecordRepository.findTodoByOperator(depart.getUserId());
        assertEquals(1, departRecordList.size());

        FlowActionRequest departRequest = new FlowActionRequest();
        departRequest.setFormData(data);
        departRequest.setRecordId(departRecordList.get(0).getId());
        departRequest.setAdvice(new FlowAdviceBody(departActions.get(0).id(), "同意", depart.getUserId()));
        factory.flowService.action(departRequest);

        departRecordList = factory.flowRecordRepository.findTodoByOperator(depart.getUserId());
        assertEquals(1, departRecordList.size());

        departRequest = new FlowActionRequest();
        departRequest.setFormData(data);
        departRequest.setRecordId(departRecordList.get(0).getId());
        departRequest.setAdvice(new FlowAdviceBody(departActions.get(0).id(), "同意", depart.getUserId()));
        factory.flowService.action(departRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(4, records.size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).toList().size());

    }

    /**
     * A-B-C 流程加签测试：A 为开始节点，B 为审批节点，C 为结束节点。
     * a 发起后流转到 b，b 加签给 c，b 通过后 c 审批，最后流程正常结束。
     */
    @Test
    void addAuditToAnotherOperator_shouldFinishFlow() {

        User initiator = new User(1, "a");
        User approver = new User(2, "b");
        User addedApprover = new User(3, "c");

        factory.userGateway.save(initiator);
        factory.userGateway.save(approver);
        factory.userGateway.save(addedApprover);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("加签流程")
                .code("addAuditFlow")
                .addField("申请人", "name", DataType.STRING)
                .addField("事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .name("A-开始")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("addAuditFlow", "name", PermissionType.WRITE)
                                .addPermission("addAuditFlow", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();

        ApprovalNode approvalNode = ApprovalNode.builder()
                .name("B-审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("addAuditFlow", "name", PermissionType.WRITE)
                                .addPermission("addAuditFlow", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder()
                .name("C-结束")
                .build();

        Workflow workflow = WorkflowBuilder.builder()
                .title("加签流程")
                .code("addAuditFlow")
                .createdOperator(initiator)
                .form(form)
                .addNode(startNode)
                .addNode(approvalNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = new HashMap<>(Map.of("name", "a", "reason", "add audit"));

        IFlowAction startAction = startNode.actionManager().getFirstAction();
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startAction.id());
        createRequest.setOperatorId(initiator.getUserId());
        factory.flowService.create(createRequest);

        List<FlowRecord> initiatorRecordList = factory.flowRecordRepository.findTodoByOperator(initiator.getUserId());
        assertEquals(1, initiatorRecordList.size());

        FlowActionRequest submitRequest = new FlowActionRequest();
        submitRequest.setFormData(data);
        submitRequest.setRecordId(initiatorRecordList.get(0).getId());
        submitRequest.setAdvice(new FlowAdviceBody(startAction.id(), "提交", initiator.getUserId()));
        factory.flowService.action(submitRequest);

        List<FlowRecord> approverRecordList = factory.flowRecordRepository.findTodoByOperator(approver.getUserId());
        assertEquals(1, approverRecordList.size());

        String processId = approverRecordList.get(0).getProcessId();
        IFlowAction addAuditAction = approvalNode.actionManager().getActionByType(ActionType.ADD_AUDIT.name());
        FlowActionRequest addAuditRequest = new FlowActionRequest();
        addAuditRequest.setFormData(data);
        addAuditRequest.setRecordId(approverRecordList.get(0).getId());

        FlowAdviceBody addAuditAdvice = new FlowAdviceBody(addAuditAction.id(), approver.getUserId());
        addAuditAdvice.setForwardOperatorIds(List.of(addedApprover.getUserId()));
        addAuditRequest.setAdvice(addAuditAdvice);
        factory.flowService.action(addAuditRequest);

        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(approver.getUserId()).size());
        assertEquals(0, factory.flowRecordRepository.findTodoByOperator(addedApprover.getUserId()).size());

        IFlowAction passAction = approvalNode.actionManager().getActionByType(ActionType.PASS.name());
        approverRecordList = factory.flowRecordRepository.findTodoByOperator(approver.getUserId());

        FlowActionRequest approverPassRequest = new FlowActionRequest();
        approverPassRequest.setFormData(data);
        approverPassRequest.setRecordId(approverRecordList.get(0).getId());
        approverPassRequest.setAdvice(new FlowAdviceBody(passAction.id(), "同意", approver.getUserId()));
        factory.flowService.action(approverPassRequest);

        assertEquals(0, factory.flowRecordRepository.findTodoByOperator(approver.getUserId()).size());
        List<FlowRecord> addedApproverRecordList = factory.flowRecordRepository.findTodoByOperator(addedApprover.getUserId());
        assertEquals(1, addedApproverRecordList.size());
        assertEquals(approvalNode.getId(), addedApproverRecordList.get(0).getNodeId());

        FlowActionRequest addedApproverPassRequest = new FlowActionRequest();
        addedApproverPassRequest.setFormData(data);
        addedApproverPassRequest.setRecordId(addedApproverRecordList.get(0).getId());
        addedApproverPassRequest.setAdvice(new FlowAdviceBody(passAction.id(), "同意", addedApprover.getUserId()));
        factory.flowService.action(addedApproverPassRequest);

        assertEquals(0, factory.flowRecordRepository.findTodoByOperator(addedApprover.getUserId()).size());

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(processId);
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).count());
        assertEquals(3, records.stream().filter(FlowRecord::isDone).count());
        assertEquals(0, factory.flowRecordRepository.findTodoRecords(processId).size());
        assertEquals(2, records.stream().filter(record -> record.getNodeId().equals(approvalNode.getId())).count());
        assertEquals(1, records.stream().filter(record -> record.getNodeId().equals(startNode.getId())).count());
        assertEquals(0, records.stream().filter(record -> record.getNodeId().equals(endNode.getId())).count());
        assertTrue(records.stream().anyMatch(record -> record.getCurrentOperatorId() == approver.getUserId()));
        assertTrue(records.stream().anyMatch(record -> record.getCurrentOperatorId() == addedApprover.getUserId()));
        assertTrue(records.stream().allMatch(record -> processId.equals(record.getProcessId())));
        assertTrue(records.stream().allMatch(record -> workflow.getCode().equals(record.getWorkCode())));
        assertTrue(records.stream().allMatch(record -> data.equals(record.getFormData())));
        assertTrue(records.stream().allMatch(record -> record.getFlowState() == FlowRecord.SATE_FLOW_FINISH));
        assertTrue(records.stream().filter(record -> record.getNodeId().equals(approvalNode.getId()))
                .allMatch(record -> ActionType.PASS.name().equals(record.getActionType())));
    }
}

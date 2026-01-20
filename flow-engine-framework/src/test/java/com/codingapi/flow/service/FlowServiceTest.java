package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.edge.FlowEdge;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.form.FormMetaBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.gateway.impl.UserGateway;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.node.nodes.BranchNodeBranchNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.*;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.flow.script.runtime.IBeanFactory;
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
    private final FlowService flowService = new FlowService(workflowRepository, userGateway, flowRecordRepository, workflowBackupRepository);

    @Test
    void create() {

        User test = new User(1, "test");
        User lorne = new User(2, "lorne");
        userGateway.save(test);
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
                .formFieldPermissionsBuilder()
                .addPermission("leave", "name", PermissionType.WRITE)
                .addPermission("leave", "days", PermissionType.WRITE)
                .addPermission("leave", "reason", PermissionType.WRITE)
                .build()
                .build();

        ApprovalNode approvalNode = ApprovalNode.builder()
                .name("经理审批")
                .operatorScript("def run(request){return [$bind.getOperatorById(2)]}")
                .formFieldPermissionsBuilder()
                .addPermission("leave", "name", PermissionType.READ)
                .addPermission("leave", "days", PermissionType.READ)
                .addPermission("leave", "reason", PermissionType.READ)
                .build()
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(test)
                .form(form)
                .addNode(startNode)
                .addNode(approvalNode)
                .addNode(endNode)
                .addEdge(new FlowEdge(startNode.getId(), approvalNode.getId()))
                .addEdge(new FlowEdge(approvalNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkId(workflow.getId());
        request.setFormData(Map.of("name", "lorne", "days", 1, "reason", "leave"));
        List<IFlowAction> actions = startNode.actions().getActions();
        request.setAdvice(new FlowAdviceBody(actions.get(0).id(), "同意", test.getUserId()));

        flowService.create(request);

        List<FlowRecord> recordList = flowRecordRepository.findTodoByOperator(test.getUserId());
        assertEquals(1, recordList.size());
    }


    /**
     * 全部通过测试
     */
    @Test
    void pass() {

        User test = new User(1, "test");
        User lorne = new User(2, "lorne");
        userGateway.save(test);
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
                .formFieldPermissionsBuilder()
                .addPermission("leave", "name", PermissionType.WRITE)
                .addPermission("leave", "days", PermissionType.WRITE)
                .addPermission("leave", "reason", PermissionType.WRITE)
                .build()
                .build();

        ApprovalNode approvalNode = ApprovalNode.builder()
                .name("经理审批")
                .operatorScript("def run(request){return [$bind.getOperatorById(2)]}")
                .formFieldPermissionsBuilder()
                .addPermission("leave", "name", PermissionType.READ)
                .addPermission("leave", "days", PermissionType.READ)
                .addPermission("leave", "reason", PermissionType.READ)
                .build()
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(test)
                .form(form)
                .addNode(startNode)
                .addNode(approvalNode)
                .addNode(endNode)
                .addEdge(new FlowEdge(startNode.getId(), approvalNode.getId()))
                .addEdge(new FlowEdge(approvalNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkId(workflow.getId());
        request.setFormData(Map.of("name", "lorne", "days", 1, "reason", "leave"));
        List<IFlowAction> actions = startNode.actions().getActions();
        request.setAdvice(new FlowAdviceBody(actions.get(0).id(), "同意", test.getUserId()));

        flowService.create(request);

        List<FlowRecord> recordList = flowRecordRepository.findTodoByOperator(test.getUserId());
        assertEquals(1, recordList.size());

        FlowActionRequest submitRequest = new FlowActionRequest();
        submitRequest.setFormData(Map.of("name", "lorne", "days", 1, "reason", "leave"));
        submitRequest.setRecordId(recordList.get(0).getId());
        submitRequest.setAdvice(new FlowAdviceBody(actions.get(0).id(), "同意", test.getUserId()));
        flowService.action(submitRequest);

        List<FlowRecord> lorneRecordList = flowRecordRepository.findTodoByOperator(lorne.getUserId());
        assertEquals(1, lorneRecordList.size());


        List<IFlowAction> lorneActions = approvalNode.actions().getActions();

        FlowActionRequest lorneRequest = new FlowActionRequest();
        lorneRequest.setFormData(Map.of("name", "lorne", "days", 1, "reason", "leave"));
        lorneRequest.setRecordId(lorneRecordList.get(0).getId());
        lorneRequest.setAdvice(new FlowAdviceBody(lorneActions.get(0).id(), "同意", lorne.getUserId()));
        flowService.action(lorneRequest);

        List<FlowRecord> records = flowRecordRepository.findRecordsByProcessId(lorneRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

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
                .formFieldPermissionsBuilder()
                .addPermission("leave", "name", PermissionType.WRITE)
                .addPermission("leave", "days", PermissionType.WRITE)
                .addPermission("leave", "reason", PermissionType.WRITE)
                .build()
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
                .operatorScript("def run(request){return [$bind.getOperatorById(2)]}")
                .formFieldPermissionsBuilder()
                .addPermission("leave", "name", PermissionType.READ)
                .addPermission("leave", "days", PermissionType.READ)
                .addPermission("leave", "reason", PermissionType.READ)
                .build()
                .build();

        ApprovalNode bossApprovalNode = ApprovalNode.builder()
                .name("经理审批")
                .operatorScript("def run(request){return [$bind.getOperatorById(3)]}")
                .formFieldPermissionsBuilder()
                .addPermission("leave", "name", PermissionType.READ)
                .addPermission("leave", "days", PermissionType.READ)
                .addPermission("leave", "reason", PermissionType.READ)
                .build()
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

        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkId(workflow.getId());
        request.setFormData(data);
        List<IFlowAction> actions = startNode.actions().getActions();
        request.setAdvice(new FlowAdviceBody(actions.get(0).id(), "同意", user.getUserId()));

        flowService.create(request);

        List<FlowRecord> recordList = flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, recordList.size());

        FlowActionRequest submitRequest = new FlowActionRequest();
        submitRequest.setFormData(data);
        submitRequest.setRecordId(recordList.get(0).getId());
        submitRequest.setAdvice(new FlowAdviceBody(actions.get(0).id(), "同意", user.getUserId()));
        flowService.action(submitRequest);

        List<FlowRecord> lorneRecordList = flowRecordRepository.findTodoByOperator(depart.getUserId());
        assertEquals(1, lorneRecordList.size());


        List<IFlowAction> lorneActions = departApprovalNode.actions().getActions();

        FlowActionRequest lorneRequest = new FlowActionRequest();
        lorneRequest.setFormData(data);
        lorneRequest.setRecordId(lorneRecordList.get(0).getId());
        lorneRequest.setAdvice(new FlowAdviceBody(lorneActions.get(0).id(), "同意", depart.getUserId()));
        flowService.action(lorneRequest);

        List<FlowRecord> records = flowRecordRepository.findRecordsByProcessId(lorneRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

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

        User test = new User(1, "test");
        User lorne = new User(2, "lorne");
        userGateway.save(test);
        userGateway.save(lorne);

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
                .build();

        StartNode startNode = StartNode
                .builder()
                .formFieldPermissionsBuilder()
                .addPermission("leave", "name", PermissionType.WRITE)
                .addPermission("leave", "days", PermissionType.WRITE)
                .addPermission("leave", "reason", PermissionType.WRITE)
                .build()
                .build();

        ApprovalNode approvalNode = ApprovalNode.builder()
                .name("经理审批")
                .operatorScript("def run(request){return [$bind.getOperatorById(2)]}")
                .formFieldPermissionsBuilder()
                .addPermission("leave", "name", PermissionType.READ)
                .addPermission("leave", "days", PermissionType.READ)
                .addPermission("leave", "reason", PermissionType.READ)
                .build()
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(test)
                .form(form)
                .addNode(startNode)
                .addNode(approvalNode)
                .addNode(endNode)
                .addEdge(new FlowEdge(startNode.getId(), approvalNode.getId()))
                .addEdge(new FlowEdge(approvalNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkId(workflow.getId());
        request.setFormData(Map.of("name", "lorne", "days", 1, "reason", "leave"));
        List<IFlowAction> startActions = startNode.actions().getActions();
        request.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", test.getUserId()));

        flowService.create(request);

        List<FlowRecord> recordList = flowRecordRepository.findTodoByOperator(test.getUserId());
        assertEquals(1, recordList.size());

        FlowActionRequest startRequest = new FlowActionRequest();
        startRequest.setFormData(Map.of("name", "lorne", "days", 1, "reason", "leave"));
        startRequest.setRecordId(recordList.get(0).getId());
        startRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", test.getUserId()));
        flowService.action(startRequest);

        List<FlowRecord> lorneRecordList = flowRecordRepository.findTodoByOperator(lorne.getUserId());
        assertEquals(1, lorneRecordList.size());


        List<IFlowAction> lorneActions = approvalNode.actions().getActions();

        FlowActionRequest lorneRequest = new FlowActionRequest();
        lorneRequest.setFormData(Map.of("name", "lorne", "days", 1, "reason", "leave"));
        lorneRequest.setRecordId(lorneRecordList.get(0).getId());
        lorneRequest.setAdvice(new FlowAdviceBody(lorneActions.get(1).id(), "不同意", lorne.getUserId()));
        flowService.action(lorneRequest);

        List<FlowRecord> testToDoList = flowRecordRepository.findTodoByOperator(test.getUserId());
        assertEquals(1, testToDoList.size());

        FlowActionRequest testRequest = new FlowActionRequest();
        testRequest.setFormData(Map.of("name", "lorne", "days", 1, "reason", "leave"));
        testRequest.setRecordId(testToDoList.get(0).getId());
        testRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", test.getUserId()));
        flowService.action(testRequest);

        lorneRecordList = flowRecordRepository.findTodoByOperator(lorne.getUserId());
        assertEquals(1, lorneRecordList.size());


        lorneRequest = new FlowActionRequest();
        lorneRequest.setFormData(Map.of("name", "lorne", "days", 1, "reason", "leave"));
        lorneRequest.setRecordId(lorneRecordList.get(0).getId());
        lorneRequest.setAdvice(new FlowAdviceBody(lorneActions.get(0).id(), "同意", lorne.getUserId()));
        flowService.action(lorneRequest);

        List<FlowRecord> records = flowRecordRepository.findRecordsByProcessId(lorneRecordList.get(0).getProcessId());
        assertEquals(4, records.size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).toList().size());

    }
}
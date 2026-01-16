package com.codingapi.flow.service;

import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.edge.FlowEdge;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.form.FormMetaBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.gateway.impl.UserGateway;
import com.codingapi.flow.node.ApprovalNode;
import com.codingapi.flow.node.EndNode;
import com.codingapi.flow.node.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.repository.*;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.Map;

class FlowServiceTest {

    private final FlowRecordRepository flowRecordRepository = new FlowRecordRepositoryImpl();
    private final UserGateway userGateway = new UserGateway();
    private final WorkflowBackupRepository workflowBackupRepository = new WorkflowBackupRepositoryImpl();
    private final WorkflowRepository workflowRepository = new WorkflowRepositoryImpl();
    private final FlowService flowService = new FlowService(workflowRepository,userGateway,flowRecordRepository,workflowBackupRepository);

    @Test
    void create() {

        User user = new User(1, "lorne");
        userGateway.save(user);

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
                .operatorScript("def run(request){return [request.getCreatedOperator()]}")
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
                .addNode(approvalNode)
                .addNode(endNode)
                .addEdge(new FlowEdge(startNode.getId(), approvalNode.getId()))
                .addEdge(new FlowEdge(approvalNode.getId(), endNode.getId()))
                .build();

        workflowRepository.save(workflow);

        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkId(workflow.getId());
        request.setFormData(Map.of("name", "lorne", "days", 1, "reason", "leave"));
        FlowAdviceBody advice = new FlowAdviceBody();
        advice.setAdvice("同意");
        advice.setAction("pass");
        advice.setOperatorId(user.getUserId());
        request.setAdvice(advice);

        flowService.create(request);
    }
}
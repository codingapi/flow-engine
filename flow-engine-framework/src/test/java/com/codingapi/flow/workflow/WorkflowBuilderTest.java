package com.codingapi.flow.workflow;

import com.codingapi.flow.context.RepositoryContext;
import com.codingapi.flow.edge.FlowEdge;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.form.FormMetaBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.ApprovalNode;
import com.codingapi.flow.node.EndNode;
import com.codingapi.flow.node.StartNode;
import com.codingapi.flow.repository.UserRepository;
import com.codingapi.flow.user.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WorkflowBuilderTest {

    private final UserRepository userRepository = new UserRepository();

    @Test
    void buildBasicWorkflow() {
        User user = new User(1, "lorne");
        userRepository.save(user);

        RepositoryContext.getInstance().setFlowOperatorRepository(userRepository);

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

        assertNotNull(workflow);
        assertEquals("请假流程", workflow.getTitle());
        assertEquals("leave", workflow.getCode());
        assertNotNull(workflow.getCreatedOperator());
        assertEquals(1, workflow.getCreatedOperator().getUserId());
        assertNotNull(workflow.getForm());
        assertEquals("请假流程", workflow.getForm().getName());
        assertNotNull(workflow.getNodes());
        assertEquals(3, workflow.getNodes().size());


        String json = workflow.toJson(true);
        System.out.println(json);

        Workflow workflowBck = Workflow.formJson(json);
        assertNotNull(workflowBck);

        assertEquals(workflow.getTitle(), workflowBck.getTitle());
        assertEquals(workflow.getCode(), workflowBck.getCode());
        assertEquals(workflow.getId(), workflowBck.getId());
        assertEquals(workflow.getCreatedOperator().getUserId(), workflowBck.getCreatedOperator().getUserId());
        assertEquals(workflow.getForm(), workflowBck.getForm());
        assertEquals(workflow.getNodes().size(), workflowBck.getNodes().size());
        assertEquals(workflow.getEdges().size(), workflowBck.getEdges().size());
        assertEquals(workflow.getSchema(), workflowBck.getSchema());
        assertEquals(workflow.getNodes().get(0).getId(), workflowBck.getNodes().get(0).getId());

    }


}

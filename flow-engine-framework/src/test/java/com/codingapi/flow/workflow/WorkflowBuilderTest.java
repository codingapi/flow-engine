package com.codingapi.flow.workflow;

import com.codingapi.flow.edge.FlowEdge;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.ApprovalNode;
import com.codingapi.flow.node.EndNode;
import com.codingapi.flow.node.StartNode;
import com.codingapi.flow.user.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WorkflowBuilderTest {

    @Test
    void buildBasicWorkflow() {
        User user = new User(1, "lorne");

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
                .build();

        StartNode startNode = new StartNode();
        startNode.setFormFieldsPermissions(FormFieldPermission
                .builder()
                .field("leave", "name", PermissionType.WRITE)
                .field("leave", "days", PermissionType.WRITE)
                .field("leave", "reason", PermissionType.WRITE)
        );

        ApprovalNode approvalNode = new ApprovalNode("经理审批");
        approvalNode.setFormFieldsPermissions(FormFieldPermission
                .builder()
                .field("leave", "name", PermissionType.READ)
                .field("leave", "days", PermissionType.READ)
                .field("leave", "reason", PermissionType.READ)
        );

        EndNode endNode = new EndNode();
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
    }

}

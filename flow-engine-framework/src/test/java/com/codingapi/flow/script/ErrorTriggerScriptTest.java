package com.codingapi.flow.script;

import com.codingapi.flow.edge.FlowEdge;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.form.FormMetaBuilder;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.ApprovalNode;
import com.codingapi.flow.node.EndNode;
import com.codingapi.flow.node.StartNode;
import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorTriggerScriptTest {

    @Test
    void execute() {
        User user = new User(1, "lorne");

        FormMeta form = FormMetaBuilder.builder()
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
        approvalNode.setOperatorScript("def run(request){return [request.getCreatedOperator()]}");
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

        FormData data = new FormData(form);
        data.getDataBody().set("name","张三").set("days",10).set("reason","事由");

        FlowSession flowSession = new FlowSession(user, form, workflow, startNode, data);

        ErrorTriggerScript errorNodeTriggerScript = ErrorTriggerScript.defaultNodeScript();
        ErrorThrow errorThrow =  errorNodeTriggerScript.execute(flowSession);
        assertTrue(errorThrow.isNode());
        assertEquals(startNode.getId(), errorThrow.getNode().getId());

        ErrorTriggerScript errorOperatorTriggerScript = ErrorTriggerScript.defaultOperatorScript();
        errorThrow =  errorOperatorTriggerScript.execute(flowSession);
        assertFalse(errorThrow.isNode());
        assertEquals(user, errorThrow.getOperators().get(0));
    }
}
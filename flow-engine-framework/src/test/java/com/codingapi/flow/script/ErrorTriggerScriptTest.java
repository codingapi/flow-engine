package com.codingapi.flow.script;

import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.script.node.ErrorTriggerScript;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorTriggerScriptTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();



    @Test
    void execute() {
        User user = new User(1, "lorne");

        factory.userGateway.save(user);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(
                                FormFieldPermissionsBuilder.builder()
                                        .addPermission("leave", "name", PermissionType.READ)
                                        .addPermission("leave", "days", PermissionType.READ)
                                        .addPermission("leave", "reason", PermissionType.READ)
                                        .build()
                        ))
                        .build())
                .build();

        ApprovalNode approvalNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(
                                FormFieldPermissionsBuilder.builder()
                                        .addPermission("leave", "name", PermissionType.READ)
                                        .addPermission("leave", "days", PermissionType.READ)
                                        .addPermission("leave", "reason", PermissionType.READ)
                                        .build()
                        ))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [request.getCreatedOperatorId()]}"))
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
                .addNode(approvalNode)
                .addNode(endNode)
                .build();

        FormData data = new FormData(form);
        data.getDataBody().set("name", "张三").set("days", 10).set("reason", "事由");

        FlowSession flowSession = FlowSession.startSession(factory.repositoryHolder,user, workflow, startNode, startNode.getActions().get(0), data, 0);

        ErrorTriggerScript errorNodeTriggerScript = ErrorTriggerScript.defaultScript();
        ErrorThrow errorThrow = errorNodeTriggerScript.execute(flowSession);
        assertTrue(errorThrow.isNode());
        assertEquals(startNode.getId(), errorThrow.getNode().getId());

        String script = """
            def run(request){ 
                return request.getCreatedOperatorId();
            }
            """;

        ErrorTriggerScript errorOperatorTriggerScript = new ErrorTriggerScript(script);
        errorThrow = errorOperatorTriggerScript.execute(flowSession);
        assertFalse(errorThrow.isNode());
        assertEquals(user, errorThrow.getOperators().get(0));
    }
}
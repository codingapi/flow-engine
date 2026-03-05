package com.codingapi.flow.script.node;

import com.codingapi.flow.form.FormData;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NodeTitleScriptTest {

    @Test
    void testExecuteWithSimpleScript() {
        NodeTitleScript script = new NodeTitleScript("def run(request){return '你有一条待办'}");
        FlowSession session = new FlowSession(
            new User(1, "张三"),
            null,
            null,
            null,
            null,
            null,
            null,
            0,
            null
        );
        String result = script.execute(session);
        assertEquals("你有一条待办", result);
    }

    @Test
    void testExecuteWithVariableScript() {
        String script = "def run(request){return request.getOperatorName() + '的审批'}";
        NodeTitleScript titleScript = new NodeTitleScript(script);

        User user = new User(1, "张三");
        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假天数", "days", "int")
                .build();

        StartNode startNode = StartNode.builder().build();
        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(endNode)
                .build();

        FormData data = new FormData(form);
        data.getDataBody().set("days", 5);

        FlowSession session = FlowSession.startSession(user, workflow, startNode, startNode.getActions().get(0), data, 0);

        String result = titleScript.execute(session);
        assertNotNull(result);
        assertEquals("张三的审批", result);
    }
}

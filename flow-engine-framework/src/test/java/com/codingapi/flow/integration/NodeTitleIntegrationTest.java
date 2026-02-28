package com.codingapi.flow.integration;

import com.codingapi.flow.form.FormData;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.form.FormMetaBuilder;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.script.node.NodeTitleScript;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Node title script integration test
 * Tests the integration between NodeTitleScript and TitleGroovyRequest
 */
class NodeTitleIntegrationTest {

    @Test
    void testTitleGenerationWithOperatorName() {
        NodeTitleScript script = new NodeTitleScript(
            "def run(request){return \"审批人：\" + request.getOperatorName()}"
        );

        User user = new User(1, "张三");
        FlowSession session = new FlowSession(user, null, null, null, null, null, null, 0, null);

        String result = script.execute(session);
        assertEquals("审批人：张三", result);
    }

    @Test
    void testTitleGenerationWithFormData() {
        NodeTitleScript script = new NodeTitleScript(
            "def run(request){return \"请假\" + request.getFormData(\"days\") + \"天\"}"
        );

        User user = new User(1, "张三");
        FormMeta form = FormMetaBuilder.builder()
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

        FlowSession session = new FlowSession(user, workflow, startNode, startNode.getActions().get(0), data, null, null, 0, null);

        String result = script.execute(session);
        assertEquals("请假5天", result);
    }

    @Test
    void testTitleGenerationWithMultipleVariables() {
        NodeTitleScript script = new NodeTitleScript(
            "def run(request){return \"你好，\" + request.getOperatorName() + \"，请假\" + request.getFormData(\"days\") + \"天\"}"
        );

        User user = new User(1, "李四");
        FormMeta form = FormMetaBuilder.builder()
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
        data.getDataBody().set("days", 3);

        FlowSession session = new FlowSession(user, workflow, startNode, startNode.getActions().get(0), data, null, null, 0, null);

        String result = script.execute(session);
        assertEquals("你好，李四，请假3天", result);
    }

    @Test
    void testTitleGenerationWithSimpleText() {
        NodeTitleScript script = new NodeTitleScript(
            "def run(request){return \"你有一条待办\"}"
        );

        User user = new User(1, "张三");
        FlowSession session = new FlowSession(user, null, null, null, null, null, null, 0, null);

        String result = script.execute(session);
        assertEquals("你有一条待办", result);
    }

    @Test
    void testTitleGenerationWithWorkflowTitle() {
        NodeTitleScript script = new NodeTitleScript(
            "def run(request){return request.getWorkflowTitle() + \" - \" + request.getOperatorName()}"
        );

        User user = new User(1, "王五");
        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .build();

        StartNode startNode = StartNode.builder().build();
        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假审批")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(endNode)
                .build();

        FlowSession session = new FlowSession(user, workflow, startNode, startNode.getActions().get(0), null, null, null, 0, null);

        String result = script.execute(session);
        assertEquals("请假审批 - 王五", result);
    }
}

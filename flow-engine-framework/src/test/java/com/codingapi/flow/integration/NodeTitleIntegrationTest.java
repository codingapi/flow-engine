package com.codingapi.flow.integration;

import com.codingapi.flow.script.node.NodeTitleScript;
import com.codingapi.flow.script.runtime.TitleGroovyRequest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

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

        TitleGroovyRequest request = new TitleGroovyRequest();
        request.setOperatorName("张三");

        String result = script.execute(request);
        assertEquals("审批人：张三", result);
    }

    @Test
    void testTitleGenerationWithFormData() {
        NodeTitleScript script = new NodeTitleScript(
            "def run(request){return \"请假\" + request.getFormData(\"days\") + \"天\"}"
        );

        TitleGroovyRequest request = new TitleGroovyRequest();
        Map<String, Object> formData = new HashMap<>();
        formData.put("days", 5);
        request.setFormData(formData);

        String result = script.execute(request);
        assertEquals("请假5天", result);
    }

    @Test
    void testTitleGenerationWithMultipleVariables() {
        NodeTitleScript script = new NodeTitleScript(
            "def run(request){return \"你好，\" + request.getOperatorName() + \"，请假\" + request.getFormData(\"days\") + \"天\"}"
        );

        TitleGroovyRequest request = new TitleGroovyRequest();
        request.setOperatorName("李四");
        Map<String, Object> formData = new HashMap<>();
        formData.put("days", 3);
        request.setFormData(formData);

        String result = script.execute(request);
        assertEquals("你好，李四，请假3天", result);
    }

    @Test
    void testTitleGenerationWithSimpleText() {
        NodeTitleScript script = new NodeTitleScript(
            "def run(request){return \"你有一条待办\"}"
        );

        TitleGroovyRequest request = new TitleGroovyRequest();

        String result = script.execute(request);
        assertEquals("你有一条待办", result);
    }

    @Test
    void testTitleGenerationWithWorkflowTitle() {
        NodeTitleScript script = new NodeTitleScript(
            "def run(request){return request.getWorkflowTitle() + \" - \" + request.getOperatorName()}"
        );

        TitleGroovyRequest request = new TitleGroovyRequest();
        request.setWorkflowTitle("请假审批");
        request.setOperatorName("王五");

        String result = script.execute(request);
        assertEquals("请假审批 - 王五", result);
    }
}

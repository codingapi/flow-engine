package com.codingapi.flow.workflow;

import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import org.junit.jupiter.api.Test;

class WorkflowBuilderTest {

    @Test
    void build() {
        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
                .build();

        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .form(form)
                .build();
    }
}
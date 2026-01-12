package com.codingapi.flow.form;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormDataTest {

    @Test
    void test() {

        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
                .build();

        FormData data = new FormData(form);
        data.set("name","张三");
        data.set("days",10);
        data.set("reason","事由");

        assertEquals("张三", data.get("name"));
        assertEquals(10, data.get("days"));
        assertEquals("事由", data.get("reason"));
    }
}
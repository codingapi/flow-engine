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
                .addSubForm(FormMetaBuilder.builder()
                        .name("审批记录")
                        .code("record")
                        .addField("审批人", "approver", "string")
                        .addField("审批结果", "result", "string")
                        .addField("审批时间", "time", "date")
                        .build())
                .build();
        FormData data = new FormData(form);
        data.getDataBody().set("name","张三").set("days",10).set("reason","事由");
        data.addSubData("record").set("approver","张三").set("result","通过").set("time","2020-01-01");
        data.addSubData("record").set("approver","李四").set("result","通过").set("time","2020-01-02");


        assertEquals("张三", data.getDataBody().get("name"));
        assertEquals(10, data.getDataBody().get("days"));
        assertEquals("事由", data.getDataBody().get("reason"));

        assertEquals(2, data.getSubDataBody().size());
        assertEquals("张三", data.getSubDataBody().get(0).get("approver"));
        assertEquals("通过", data.getSubDataBody().get(0).get("result"));
        assertEquals("2020-01-01", data.getSubDataBody().get(0).get("time"));
        assertEquals("李四", data.getSubDataBody().get(1).get("approver"));
        assertEquals("通过", data.getSubDataBody().get(1).get("result"));
        assertEquals("2020-01-02", data.getSubDataBody().get(1).get("time"));
    }
}
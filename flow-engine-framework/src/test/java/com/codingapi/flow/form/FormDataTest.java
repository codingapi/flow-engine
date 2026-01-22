package com.codingapi.flow.form;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormDataTest {

    /**
     * 表单数据测试
     */
    @Test
    void dataTest() {
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
        data.getDataBody().set("name", "张三").set("days", 10).set("reason", "事由");
        data.addSubDataBody("record").set("approver", "张三").set("result", "通过").set("time", "2020-01-01");
        data.addSubDataBody("record").set("approver", "李四").set("result", "通过").set("time", "2020-01-02");

        assertEquals("张三", data.getDataBody().get("name"));
        assertEquals(10, data.getDataBody().get("days"));
        assertEquals("事由", data.getDataBody().get("reason"));

        Map<String, Object> mainData = data.toMapData();

        data.reset(mainData);

        assertEquals("张三", mainData.get("name"));
        assertEquals(10, mainData.get("days"));
        assertEquals("事由", mainData.get("reason"));

        data.getDataBody().set("name", "张三-112233");
        List<FormData.DataBody> subDataBody = data.getSubDataBody("record");
        assertEquals(2, subDataBody.size());
        assertEquals(1, data.countSubDataBody());

        assertEquals("张三-112233", data.getDataBody().get("name"));

        assertEquals("张三", subDataBody.get(0).get("approver"));
        assertEquals("通过", subDataBody.get(0).get("result"));
        assertEquals("2020-01-01", subDataBody.get(0).get("time"));
        assertEquals("李四", subDataBody.get(1).get("approver"));
        assertEquals("通过", subDataBody.get(1).get("result"));
        assertEquals("2020-01-02", subDataBody.get(1).get("time"));
    }
}
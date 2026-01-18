package com.codingapi.flow.pojo.body;

import com.alibaba.fastjson.JSONObject;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormDataBodyTest {


    @Test
    void test() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", "张三");
        jsonObject.put("age", 18);
        jsonObject.put("sex", "男");

        List<JSONObject> records = new ArrayList<>();
        JSONObject record = new JSONObject();
        record.put("name", "张三");
        record.put("age", 18);
        record.put("sex", "男");
        records.add(record);
        jsonObject.put("records", records);

        JSONObject request = new JSONObject();
        request.put("formData", jsonObject);
        request.put("workId", "123123");

        FlowCreateRequest formDataBody = JSONObject.parseObject(request.toJSONString(), FlowCreateRequest.class);
        assertEquals("123123", formDataBody.getWorkId());
        assertEquals("张三", formDataBody.getFormData().get("name"));

    }

}
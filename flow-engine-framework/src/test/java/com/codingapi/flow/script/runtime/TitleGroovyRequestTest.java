package com.codingapi.flow.script.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

class TitleGroovyRequestTest {

    @Test
    void testGetOperatorName() {
        TitleGroovyRequest request = new TitleGroovyRequest();
        request.setOperatorName("张三");
        assertEquals("张三", request.getOperatorName());
    }

    @Test
    void testGetFormData() {
        TitleGroovyRequest request = new TitleGroovyRequest();
        Map<String, Object> formData = new HashMap<>();
        formData.put("days", 5);
        request.setFormData(formData);
        assertEquals(5, request.getFormData("days"));
    }
}

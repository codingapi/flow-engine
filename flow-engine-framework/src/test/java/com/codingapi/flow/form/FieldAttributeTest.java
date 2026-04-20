package com.codingapi.flow.form;

import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FieldAttributeTest {


    @Test
    void convert(){

        String attribute1 = """
                {
                   "key":"123",
                   "label":"123",
                   "value":{
                      "data":"123"
                   }
                }
                """;

        String attribute2 = """
                {
                   "key":"123",
                   "label":"123",
                   "value":[
                      {
                        "data":"123"
                      },
                      {
                        "data":"123"
                      }
                   ]
                }
                """;

        FieldAttribute attributeObj1 =  JSONObject.parseObject(attribute1,FieldAttribute.class);
        FieldAttribute attributeObj2 =  JSONObject.parseObject(attribute2,FieldAttribute.class);

        assertEquals("{\"data\":\"123\"}", attributeObj1.getValue().toString());
        assertEquals("[{\"data\":\"123\"},{\"data\":\"123\"}]", attributeObj2.getValue().toString());

    }
}
package com.codingapi.flow.form;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 流程表单数据
 */
@Getter
public class FormData {

    private final FlowForm form;
    private final Map<String, Object> data;
    private final Map<String, String> fieldTypes;

    public FormData(FlowForm form) {
        this.form = form;
        this.data = new HashMap<>();
        this.fieldTypes =  this.form.getFieldTypeMaps();
    }

    public void set(String form,String key, Object value) {
        String id = form + "." + key;
        String type = this.fieldTypes.get(id);
        if (type == null) {
            throw new RuntimeException("key:" + key + " not found");
        }
        this.data.put(key, value);
    }

    public Object get(String form,String key) {
        String id = form + "." + key;
        return this.data.get(id);
    }

}

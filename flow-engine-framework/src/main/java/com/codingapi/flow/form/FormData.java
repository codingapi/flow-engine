package com.codingapi.flow.form;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程表单数据
 */
@Getter
public class FormData {

    private final FormMeta form;
    private final Map<String, Object> data;
    private final List<FormData> subData;
    private final Map<String, String> fieldTypes;

    public FormData(FormMeta form) {
        this.form = form;
        this.data = new HashMap<>();
        this.subData = new ArrayList<>();
        this.fieldTypes =  this.form.getMainFieldTypeMaps();
        if(form.getSubForms()!=null) {
            for (FormMeta subForm : form.getSubForms()) {
                this.subData.add(new FormData(subForm));
            }
        }
    }

    public void set(String key, Object value) {
        String id = form.getCode() + "." + key;
        String type = this.fieldTypes.get(id);
        if (type == null) {
            throw new RuntimeException("key:" + key + " not found");
        }
        this.data.put(id, value);
    }

    public Object get(String key) {
        String id = form.getCode() + "." + key;
        return this.data.get(id);
    }

}

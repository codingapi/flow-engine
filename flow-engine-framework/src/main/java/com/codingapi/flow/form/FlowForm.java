package com.codingapi.flow.form;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程表单
 */
@Setter
@Getter
public class FlowForm {

    /**
     * 表单名称
     */
    private String name;
    /**
     * 表单编号 唯一标识
     */
    private String code;
    /**
     * 表单字段
     */
    private List<FormFieldMeta> fields;

    /**
     * 子表单
     */
    private List<FlowForm> subForms;

    /**
     * 获取表单字段名称
     */
    public List<String> getFieldNames() {
    	return fields.stream().map(FormFieldMeta::getName).toList();
    }

    /**
     * 获取表单字段
     * @param fieldName 字段名称
     * @return 表单字段
     */
    public FormFieldMeta getField(String fieldName) {
    	return fields.stream().filter(field -> field.getName().equals(fieldName)).findFirst().orElse(null);
    }

    private void initFormFieldTypes(FlowForm form,Map<String,String> types) {
        for (FormFieldMeta field : form.getFields()) {
            String key = form.getCode() + "." + field.getCode();
            String type = field.getType();
            types.put(key, type);
        }
    }

    /**
     * 获取表单字段类型
     * @return 表单字段类型
     */
    public Map<String,String> getFieldTypeMaps(){
        Map<String,String> types = new HashMap<>();
        List<FlowForm> forms = this.getSubForms();
        if (forms == null) {
            forms = new ArrayList<>();
        }
        forms.add(this);
        for (FlowForm subForm : forms) {
            this.initFormFieldTypes(subForm, types);
        }
        return types;
    }

}

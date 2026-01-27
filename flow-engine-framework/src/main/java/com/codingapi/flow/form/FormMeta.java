package com.codingapi.flow.form;

import com.alibaba.fastjson.JSON;
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
public class FormMeta {

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
    private List<FormMeta> subForms;


    public boolean isSubForm(String formCode) {
        if (subForms != null) {
            return subForms.stream().anyMatch(form -> form.getCode().equals(formCode));
        }
        return false;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FormMeta form) {
            String json = JSON.toJSONString(form.toMap());
            return json.equals(JSON.toJSONString(this.toMap()));
        }
        return super.equals(obj);
    }

    /**
     * 转换成map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("code", code);
        map.put("fields", fields);
        map.put("subForms", subForms != null ? subForms.stream().map(FormMeta::toMap).toList() : null);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static FormMeta fromMap(Map<String, Object> map) {
        FormMeta form = new FormMeta();
        List<Map<String, Object>> fields = (List<Map<String, Object>>) map.get("fields");
        List<FormFieldMeta> fieldList = new ArrayList<>();
        if(fields!=null && !fields.isEmpty()) {
            for (Map<String, Object> field : fields) {
                FormFieldMeta fieldMeta = new FormFieldMeta();
                fieldMeta.setName((String) field.get("name"));
                fieldMeta.setCode((String) field.get("code"));
                fieldMeta.setType((String) field.get("type"));
                fieldMeta.setRequired(Boolean.TRUE.equals(field.get("required")));
                fieldMeta.setDefaultValue((String) field.get("defaultValue"));
                fieldList.add(fieldMeta);
            }
        }
        form.setName((String) map.get("name"));
        form.setCode((String) map.get("code"));
        form.setFields(fieldList);

        List<Map<String, Object>> subForms = (List<Map<String, Object>>) map.get("subForms");
        if (subForms != null) {
            List<FormMeta> subFormList = new ArrayList<>();
            for (Map<String, Object> subForm : subForms) {
                subFormList.add(fromMap(subForm));
            }
            form.setSubForms(subFormList);
        }
        return form;
    }

    /**
     * 获取表单字段名称
     */
    public List<String> getFieldNames() {
        return fields.stream().map(FormFieldMeta::getName).toList();
    }

    /**
     * 获取表单字段
     *
     * @param fieldName 字段名称
     * @return 表单字段
     */
    public FormFieldMeta getField(String fieldName) {
        return fields.stream().filter(field -> field.getName().equals(fieldName)).findFirst().orElse(null);
    }

    private void initFormFieldTypes(FormMeta form, Map<String, String> types) {
        for (FormFieldMeta field : form.getFields()) {
            String key = form.getCode() + "." + field.getCode();
            String type = field.getType();
            types.put(key, type);
        }
    }

    /**
     * 获取表单字段类型
     *
     * @return 表单字段类型
     */
    public Map<String, String> getAllFieldTypeMaps() {
        Map<String, String> types = new HashMap<>();
        List<FormMeta> forms = this.getSubForms();
        if (forms == null) {
            forms = new ArrayList<>();
        }
        forms.add(this);
        for (FormMeta subForm : forms) {
            this.initFormFieldTypes(subForm, types);
        }
        return types;
    }


    /**
     * 获取表单字段类型
     *
     * @return 表单字段类型
     */
    public Map<String, String> getMainFieldTypeMaps() {
        Map<String, String> types = new HashMap<>();
        List<FormMeta> forms = new ArrayList<>();
        forms.add(this);
        for (FormMeta subForm : forms) {
            this.initFormFieldTypes(subForm, types);
        }
        return types;
    }

    public FormMeta getSubForm(String formCode) {
        return this.subForms.stream().filter(form -> form.getCode().equals(formCode)).findFirst().orElse(null);
    }
}

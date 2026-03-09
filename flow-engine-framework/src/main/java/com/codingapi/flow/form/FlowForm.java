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
    private List<FormField> fields;

    /**
     * 子表单
     */
    private List<FlowForm> subForms;


    public boolean isSubForm(String formCode) {
        if (subForms != null) {
            return subForms.stream().anyMatch(form -> form.getCode().equals(formCode));
        }
        return false;
    }

    /**
     * 转换成map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("code", code);
        map.put("fields", fields);
        if (subForms != null && !subForms.isEmpty()) {
            map.put("subForms", subForms.stream().map(FlowForm::toMap).toList());
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public static FlowForm fromMap(Map<String, Object> map) {
        FlowForm form = new FlowForm();
        List<Map<String, Object>> fields = (List<Map<String, Object>>) map.get("fields");
        List<FormField> fieldList = new ArrayList<>();
        if (fields != null && !fields.isEmpty()) {
            for (Map<String, Object> field : fields) {
                FormField fieldMeta = new FormField();
                fieldMeta.setId((String) field.get("id"));
                fieldMeta.setName((String) field.get("name"));
                fieldMeta.setCode((String) field.get("code"));
                fieldMeta.setType(DataType.valueOf((String) field.get("type")));
                fieldMeta.setRequired(Boolean.TRUE.equals(field.get("required")));
                fieldMeta.setHidden(Boolean.TRUE.equals(field.get("hidden")));
                fieldMeta.setDefaultValue((String) field.get("defaultValue"));
                fieldMeta.setPlaceholder((String) field.get("placeholder"));
                fieldMeta.setTooltip((String) field.get("tooltip"));
                fieldMeta.setHelp((String) field.get("help"));
                fieldList.add(fieldMeta);
            }
        }
        form.setName((String) map.get("name"));
        form.setCode((String) map.get("code"));
        form.setFields(fieldList);

        List<Map<String, Object>> subForms = (List<Map<String, Object>>) map.get("subForms");
        if (subForms != null) {
            List<FlowForm> subFormList = new ArrayList<>();
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
    public List<String> loadFieldNames() {
        return fields.stream().map(FormField::getName).toList();
    }

    /**
     * 获取表单字段
     *
     * @param fieldName 字段名称
     * @return 表单字段
     */
    public FormField getField(String fieldName) {
        return fields.stream().filter(field -> field.getName().equals(fieldName)).findFirst().orElse(null);
    }

    private void initFormFieldTypes(FlowForm form, Map<String, DataType> types) {
        for (FormField field : form.getFields()) {
            String key = form.getCode() + "." + field.getCode();
            DataType type = field.getType();
            types.put(key, type);
        }
    }

    /**
     * 获取表单字段类型
     *
     * @return 表单字段类型
     */
    public Map<String, DataType> loadAllFieldTypeMaps() {
        Map<String, DataType> types = new HashMap<>();
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


    /**
     * 获取表单字段类型
     *
     * @return 表单字段类型
     */
    public Map<String, DataType> loadMainFieldTypeMaps() {
        Map<String, DataType> types = new HashMap<>();
        List<FlowForm> forms = new ArrayList<>();
        forms.add(this);
        for (FlowForm subForm : forms) {
            this.initFormFieldTypes(subForm, types);
        }
        return types;
    }

    public FlowForm getSubForm(String formCode) {
        return this.subForms.stream().filter(form -> form.getCode().equals(formCode)).findFirst().orElse(null);
    }
}

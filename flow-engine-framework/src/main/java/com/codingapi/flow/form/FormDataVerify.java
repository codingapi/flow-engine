package com.codingapi.flow.form;

import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.form.permission.PermissionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 表单数据校验逻辑
 */
public class FormDataVerify {

    private final FlowForm formMeta;
    private final List<FormFieldPermission> fieldPermissions;
    private final Map<String, Object> latestData;

    public FormDataVerify(FormData formData, List<FormFieldPermission> fieldPermissions) {
        this.latestData = formData.toMapData();
        this.fieldPermissions = new ArrayList<>();
        if (fieldPermissions != null && !fieldPermissions.isEmpty()) {
            this.fieldPermissions.addAll(fieldPermissions);
        }
        this.formMeta = this.convert(formData.getFlowForm());
    }

    private FlowForm convert(FlowForm form) {
        if (form == null) {
            return null;
        }
        List<FlowForm> list = new ArrayList<>();
        List<FlowForm> subForms = form.getSubForms();

        List<FormField> fields = form.getFields().stream()
                .map(item -> this.mapFormFiled(form.getCode(), item))
                .toList();
        form.setFields(fields);

        if (subForms != null && !subForms.isEmpty()) {
            for (FlowForm subForm : subForms) {
                list.add(this.convert(subForm));
            }
            form.setSubForms(list);
        }
        return form;
    }


    private FormField mapFormFiled(String formCode, FormField formField) {
        if (formField != null) {
            FormFieldPermission permission = this.fieldPermissions.stream().filter(item -> {
                if (item.getFieldCode().equals(formField.getCode()) && item.getFormCode().equals(formCode)) {
                    return true;
                }
                return false;
            }).findFirst().orElse(null);

            if (permission != null) {
                if (permission.getType() == PermissionType.HIDDEN) {
                    formField.setRequired(false);
                    formField.setHidden(true);
                    return formField;
                }
                if (permission.getType() == PermissionType.READ) {
                    formField.setRequired(false);
                    formField.setReadonly(true);
                }
                return formField;
            }
        }
        return formField;
    }


    private void verifyData(FlowForm formMeta, Map<String, Object> latestData) {
        List<FormField> fields = formMeta.getFields();
        if (fields != null) {
            for (FormField formField : fields) {
                Object latest = latestData.get(formField.getCode());

                if (formField.isRequired()) {
                    if (latest == null) {
                        throw FlowValidationException.fieldNotFound(formField.getName());
                    }
                }

            }
        }
    }


    public void verify() {
        this.verifyData(formMeta, latestData);

        List<FlowForm> subForms = this.formMeta.getSubForms();
        if (subForms != null) {
            for (FlowForm subForm : subForms) {
                String subFormCode = subForm.getCode();
                List<Map<String, Object>> subFormLatestDataList = (List<Map<String, Object>>) latestData.get(subFormCode);
                if (subFormLatestDataList != null) {
                    for (Map<String, Object> latestData : subFormLatestDataList) {
                        this.verifyData(subForm, latestData);
                    }
                }
            }
        }
    }

}

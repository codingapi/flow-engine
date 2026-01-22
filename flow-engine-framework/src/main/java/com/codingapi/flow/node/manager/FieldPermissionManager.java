package com.codingapi.flow.node.manager;

import com.codingapi.flow.exception.FlowConfigException;
import com.codingapi.flow.exception.FlowPermissionException;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.form.permission.PermissionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 表单字段权限管理
 */
@AllArgsConstructor
public class FieldPermissionManager {

    @Getter
    private final List<FormFieldPermission> permissions;

    /**
     * 验证字段权限是否存在
     *
     * @param form 表单元数据
     */
    public void verifyPermissions(FormMeta form) {
        Map<String, String> fieldTypes = form.getAllFieldTypeMaps();
        for (FormFieldPermission permission : permissions) {
            String key = permission.getFormCode() + "." + permission.getFieldName();
            if (!fieldTypes.containsKey(key)) {
                throw FlowPermissionException.fieldNotFound(key);
            }
        }
    }

    /**
     * 验证表单数据
     *
     * @param formMeta    表单元数据
     * @param currentData 当前表单数据
     * @param latestData  最新表单数据
     */
    @SuppressWarnings("unchecked")
    public void verifyFormData(FormMeta formMeta, Map<String, Object> currentData, Map<String, Object> latestData) {
        for (FormFieldPermission permission : permissions) {
            // 子表
            if (formMeta.isSubForm(permission.getFormCode())) {
                if (permission.getType() == PermissionType.READ) {
                    List<Map<String, Object>> currentSubFormData = (List<Map<String, Object>>) currentData.get(permission.getFormCode());
                    List<Map<String, Object>> latestSubFormData = (List<Map<String, Object>>) latestData.get(permission.getFormCode());
                    if (currentSubFormData == null || latestSubFormData == null) {
                        throw FlowConfigException.formConfigError(permission.getFormCode(), "is not a sub form");
                    }

                    if (currentSubFormData.size() != latestSubFormData.size()) {
                        throw FlowConfigException.formConfigError(permission.getFormCode(), "size is not equal");
                    }

                    for (int i = 0; i < currentSubFormData.size(); i++) {
                        Map<String, Object> currentSubFormItem = currentSubFormData.get(i);
                        Map<String, Object> latestSubFormItem = latestSubFormData.get(i);
                        Object currentValue = currentSubFormItem.get(permission.getFieldName());
                        Object latestValue = latestSubFormItem.get(permission.getFieldName());
                        if (!currentValue.equals(latestValue)) {
                            throw FlowPermissionException.fieldReadOnly(permission.getFieldName());
                        }
                    }
                }
            } else {
                // 在只读权限下不允许修改数据
                if (permission.getType() == PermissionType.READ) {
                    Object currentValue = currentData.get(permission.getFieldName());
                    Object latestValue = latestData.get(permission.getFieldName());
                    if (!currentValue.equals(latestValue)) {
                        throw FlowPermissionException.fieldReadOnly(permission.getFieldName());
                    }
                }
            }
        }
    }
}

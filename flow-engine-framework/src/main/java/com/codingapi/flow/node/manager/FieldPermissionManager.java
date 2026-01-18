package com.codingapi.flow.node.manager;

import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.form.permission.FormFieldPermission;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class FieldPermissionManager {

    @Getter
    private final List<FormFieldPermission> permissions;

    public void verifyPermissions(FormMeta form) {
        Map<String, String> fieldTypes = form.getAllFieldTypeMaps();
        for (FormFieldPermission permission : permissions) {
            String key = permission.getFormCode() + "." + permission.getFieldName();
            if (!fieldTypes.containsKey(key)) {
                throw new IllegalArgumentException("field " + key + " not found in form " + form.getName());
            }
        }
    }
}

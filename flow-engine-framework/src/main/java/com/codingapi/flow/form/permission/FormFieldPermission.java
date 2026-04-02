package com.codingapi.flow.form.permission;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class FormFieldPermission {

    private String formCode;
    private String fieldCode;
    private PermissionType type;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("formCode", formCode);
        map.put("fieldCode", fieldCode);
        map.put("type", type.name());
        return map;
    }

    public static FormFieldPermission fromMap(Map<String, Object> map) {
        FormFieldPermission permission = new FormFieldPermission();
        permission.setFormCode((String) map.get("formCode"));
        permission.setFieldCode((String) map.get("fieldCode"));
        permission.setType(PermissionType.valueOf((String) map.get("type")));
        return permission;
    }

    public boolean isField(String formCode, String fieldCode) {
        return this.getFormCode().equalsIgnoreCase(formCode) && this.getFieldCode().equalsIgnoreCase(fieldCode);
    }
}

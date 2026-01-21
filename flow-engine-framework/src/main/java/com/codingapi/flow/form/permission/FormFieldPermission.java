package com.codingapi.flow.form.permission;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class FormFieldPermission {

    private String formCode;
    private String fieldName;
    private PermissionType type;

    public Map<String,Object> toMap() {
        Map<String,Object> map = new HashMap<>();
        map.put("formCode",formCode);
        map.put("fieldName",fieldName);
        map.put("type",type.name());
        return map;
    }

    public static FormFieldPermission fromMap(Map<String,Object> map) {
        FormFieldPermission permission = new FormFieldPermission();
        permission.setFormCode((String) map.get("formCode"));
        permission.setFieldName((String) map.get("fieldName"));
        permission.setType(PermissionType.valueOf((String) map.get("type")));
        return permission;
    }
}

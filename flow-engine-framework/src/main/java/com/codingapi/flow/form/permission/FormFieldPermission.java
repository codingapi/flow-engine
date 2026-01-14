package com.codingapi.flow.form.permission;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FormFieldPermission {

    private String formCode;
    private String fieldName;
    private PermissionType type;
}

package com.codingapi.flow.form.permission;

import com.codingapi.flow.form.FormFieldMeta;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FormFieldPermission {

    private FormFieldMeta field;
    private PermissionType type;
    private String formCode;

}

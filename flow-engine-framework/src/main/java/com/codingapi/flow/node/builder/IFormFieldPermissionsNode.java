package com.codingapi.flow.node.builder;

import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.node.manager.FieldPermissionManager;

import java.util.List;

public interface IFormFieldPermissionsNode {

    void setFormFieldPermissions(List<FormFieldPermission> formFieldPermissions);

    FieldPermissionManager formFieldsPermissionsManager();
}

package com.codingapi.flow.node.builder;

import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.form.permission.PermissionType;

import java.util.ArrayList;
import java.util.List;

public class FormFieldPermissionsBuilder  {

    private final List<FormFieldPermission> permissions;

    private FormFieldPermissionsBuilder() {
        this.permissions = new ArrayList<>();
    }

    public static FormFieldPermissionsBuilder builder() {
        return new FormFieldPermissionsBuilder();
    }

    public FormFieldPermissionsBuilder addPermission(String form, String name, PermissionType type) {
        FormFieldPermission permission = new FormFieldPermission();
        permission.setFormCode(form);
        permission.setFieldName(name);
        permission.setType(type);
        permissions.add(permission);
        return this;
    }

    public List<FormFieldPermission> build() {
        return permissions;
    }

}

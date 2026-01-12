package com.codingapi.flow.form.permission;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class FormFieldPermission {

    private String formCode;
    private String fieldName;
    private PermissionType type;


    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final List<FormFieldPermission> permissions = new ArrayList<>();

        private Builder(){
        }

        public List<FormFieldPermission> build() {
            return permissions;
        }

        public Builder field(String form, String name, PermissionType type) {
            FormFieldPermission permission = new FormFieldPermission();
            permission.setFormCode(form);
            permission.setFieldName(name);
            permission.setType(type);
            permissions.add(permission);
            return this;
        }
    }

}

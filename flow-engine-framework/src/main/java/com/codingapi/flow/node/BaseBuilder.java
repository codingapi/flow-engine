package com.codingapi.flow.node;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.strategy.INodeStrategy;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseBuilder<N extends BaseNode> {

    protected final N node;

    public BaseBuilder(N node) {
        this.node = node;
    }

    public BaseBuilder<N> id(String id) {
        node.setId(id);
        return this;
    }

    public BaseBuilder<N> actions(List<IFlowAction> actions) {
        node.setActions(actions);
        return this;
    }

    public BaseBuilder<N> name(String name) {
        node.setName(name);
        return this;
    }

    public BaseBuilder<N> view(String view) {
        node.setView(view);
        return this;
    }

    public BaseBuilder<N> nodeStrategies(List<INodeStrategy> nodeStrategies) {
        node.setNodeStrategies(nodeStrategies);
        return this;
    }


    public BaseBuilder<N> operatorScript(String operatorScript) {
        node.setOperatorScript(operatorScript);
        return this;
    }

    public BaseBuilder<N> nodeTitleScript(String nodeTitleScript) {
        node.setNodeTitleScript(nodeTitleScript);
        return this;
    }

    public BaseBuilder<N> errorTriggerScript(String errorTriggerScript) {
        node.setErrorTriggerScript(errorTriggerScript);
        return this;
    }

    public BaseBuilder<N> formFieldsPermissions(List<FormFieldPermission> permissions) {
        node.setFormFieldPermissions(permissions);
        return this;
    }

    public FormFieldPermissionsBuilder<N> formFieldPermissionsBuilder() {
        return new FormFieldPermissionsBuilder<N>(this, node);
    }

    public N build() {
        return node;
    }

    public static class FormFieldPermissionsBuilder<T extends BaseNode> {

        private final T node;
        private final BaseBuilder<T> baseBuilder;
        private final List<FormFieldPermission> permissions;

        public FormFieldPermissionsBuilder(BaseBuilder<T> baseBuilder, T node) {
            this.baseBuilder = baseBuilder;
            this.node = node;
            this.permissions = new ArrayList<>();
        }

        public FormFieldPermissionsBuilder<T> addPermission(String form, String name, PermissionType type) {
            FormFieldPermission permission = new FormFieldPermission();
            permission.setFormCode(form);
            permission.setFieldName(name);
            permission.setType(type);
            permissions.add(permission);
            return this;
        }

        public BaseBuilder<T> build() {
            node.setFormFieldPermissions(this.permissions);
            return baseBuilder;
        }
    }
}

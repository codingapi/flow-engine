package com.codingapi.flow.node;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.strategy.INodeStrategy;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseBuilder<B extends BaseBuilder<B, N>, N extends BaseNode> {

    protected final N node;

    public BaseBuilder(N node) {
        this.node = node;
    }

    public B id(String id) {
        node.setId(id);
        return (B)this;
    }

    public B actions(List<IFlowAction> actions) {
        node.setActions(actions);
        return (B)this;
    }

    public B addAction(IFlowAction action) {
        node.addAction(action);
        return (B)this;
    }

    public B name(String name) {
        node.setName(name);
        return (B)this;
    }

    public B view(String view) {
        node.setView(view);
        return (B)this;
    }

    public B nodeStrategies(List<INodeStrategy> nodeStrategies) {
        node.setNodeStrategies(nodeStrategies);
        return (B)this;
    }


    public B operatorScript(String operatorScript) {
        node.setOperatorScript(operatorScript);
        return (B)this;
    }

    public B nodeTitleScript(String nodeTitleScript) {
        node.setNodeTitleScript(nodeTitleScript);
        return (B)this;
    }

    public B errorTriggerScript(String errorTriggerScript) {
        node.setErrorTriggerScript(errorTriggerScript);
        return (B)this;
    }

    public B formFieldsPermissions(List<FormFieldPermission> permissions) {
        node.setFormFieldPermissions(permissions);
        return (B)this;
    }

    public FormFieldPermissionsBuilder<B,N> formFieldPermissionsBuilder() {
        return new FormFieldPermissionsBuilder<>(this, node);
    }

    public N build() {
        return node;
    }

    public static class FormFieldPermissionsBuilder<B extends BaseBuilder<B, N>,N extends BaseNode> {

        private final N node;
        private final BaseBuilder<B,N> baseBuilder;
        private final List<FormFieldPermission> permissions;

        public FormFieldPermissionsBuilder(BaseBuilder<B,N> baseBuilder, N node) {
            this.baseBuilder = baseBuilder;
            this.node = node;
            this.permissions = new ArrayList<>();
        }

        public FormFieldPermissionsBuilder<B,N> addPermission(String form, String name, PermissionType type) {
            FormFieldPermission permission = new FormFieldPermission();
            permission.setFormCode(form);
            permission.setFieldName(name);
            permission.setType(type);
            permissions.add(permission);
            return this;
        }

        public BaseBuilder<B,N> build() {
            node.setFormFieldPermissions(this.permissions);
            return baseBuilder;
        }
    }
}

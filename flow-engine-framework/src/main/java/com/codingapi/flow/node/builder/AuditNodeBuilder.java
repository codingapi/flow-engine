package com.codingapi.flow.node.builder;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.node.BaseAuditNode;
import com.codingapi.flow.strategy.INodeStrategy;

import java.util.List;

public abstract class AuditNodeBuilder<B extends AuditNodeBuilder<B, N>, N extends BaseAuditNode> {

    private final N node;

    public AuditNodeBuilder(N node) {
        this.node = node;
    }

    public B id(String id) {
        node.setId(id);
        return (B) this;
    }

    public B actions(List<IFlowAction> actions) {
        node.setActions(actions);
        return (B) this;
    }

    public B addAction(IFlowAction action) {
        node.addAction(action);
        return (B) this;
    }

    public B name(String name) {
        node.setName(name);
        return (B) this;
    }

    public B view(String view) {
        node.setView(view);
        return (B) this;
    }

    public B nodeStrategies(List<INodeStrategy> nodeStrategies) {
        node.setNodeStrategies(nodeStrategies);
        return (B) this;
    }


    public B operatorScript(String operatorScript) {
        node.setOperatorScript(operatorScript);
        return (B) this;
    }

    public B nodeTitleScript(String nodeTitleScript) {
        node.setNodeTitleScript(nodeTitleScript);
        return (B) this;
    }

    public B errorTriggerScript(String errorTriggerScript) {
        node.setErrorTriggerScript(errorTriggerScript);
        return (B) this;
    }

    public B formFieldsPermissions(List<FormFieldPermission> permissions) {
        node.setFormFieldPermissions(permissions);
        return (B) this;
    }

    public N build() {
        return node;
    }
}


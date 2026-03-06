package com.codingapi.flow.form;

import java.util.ArrayList;

public class FlowFormBuilder {

    private FlowForm flowForm = new FlowForm();

    private FlowFormBuilder() {
    }

    public static FlowFormBuilder builder() {
        return new FlowFormBuilder();
    }

    public FlowFormBuilder name(String name) {
        flowForm.setName(name);
        return this;
    }

    public FlowFormBuilder code(String code) {
        flowForm.setCode(code);
        return this;
    }

    public FlowFormBuilder addField(String name, String code, DataType type) {
        FormField field = new FormField();
        field.setName(name);
        field.setCode(code);
        field.setType(type);
        field.setRequired(true);
        field.setDefaultValue(null);
        return this.addField(field);
    }

    public FlowFormBuilder addField(FormField field) {
        if (flowForm.getFields() == null) {
            flowForm.setFields(new ArrayList<>());
        }
        flowForm.getFields().add(field);
        return this;
    }

    public FlowFormBuilder addSubForm(FlowForm subForm) {
        if (flowForm.getSubForms() == null) {
            flowForm.setSubForms(new ArrayList<>());
        }
        flowForm.getSubForms().add(subForm);
        return this;
    }

    public FlowForm build() {
        FlowForm result = flowForm;
        flowForm = new FlowForm();
        return result;
    }
}

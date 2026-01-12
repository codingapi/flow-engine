package com.codingapi.flow.form;

import java.util.ArrayList;

public class FormMetaBuilder {

    private final static FormMetaBuilder instance = new FormMetaBuilder();

    private FormMeta formMeta = new FormMeta();

    private FormMetaBuilder() {
    }

    public static FormMetaBuilder builder() {
        return instance;
    }

    public FormMetaBuilder name(String name) {
        formMeta.setName(name);
        return this;
    }

    public FormMetaBuilder code(String code) {
        formMeta.setCode(code);
        return this;
    }

    public FormMetaBuilder addField(String name, String code, String type) {
        FormFieldMeta field = new FormFieldMeta();
        field.setName(name);
        field.setCode(code);
        field.setType(type);
        field.setNullable(true);
        field.setDefaultValue(null);
        return this.addField(field);
    }

    public FormMetaBuilder addField(FormFieldMeta field) {
        if (formMeta.getFields() == null) {
            formMeta.setFields(new ArrayList<>());
        }
        formMeta.getFields().add(field);
        return this;
    }

    public FormMetaBuilder addSubForm(FormMeta subForm) {
        if (formMeta.getSubForms() == null) {
            formMeta.setSubForms(new ArrayList<>());
        }
        formMeta.getSubForms().add(subForm);
        return this;
    }

    public FormMeta build() {
        FormMeta result = formMeta;
        formMeta = new FormMeta();
        return result;
    }
}

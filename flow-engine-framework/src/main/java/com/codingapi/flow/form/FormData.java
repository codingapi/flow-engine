package com.codingapi.flow.form;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程表单数据
 */
@Getter
public class FormData {

    private final FormMeta formMeta;
    private final DataBody dataBody;
    private final List<DataBody> subDataBody;
    private final List<FormData> subData;

    public FormData(FormMeta form) {
        this.formMeta = form;
        this.subData = new ArrayList<>();
        this.dataBody = new DataBody(form);
        this.subDataBody = new ArrayList<>();
        if(form.getSubForms()!=null) {
            for (FormMeta subForm : form.getSubForms()) {
                this.subData.add(new FormData(subForm));
            }
        }
    }

    public DataBody addSubData(String formCode) {
        FormMeta subFormMeta = getSubFormMeta(formCode);
        if(subFormMeta==null){
            return null;
        }
        DataBody subData = new DataBody(subFormMeta);
        this.subDataBody.add(subData);
        return subData;
    }


    private FormMeta getSubFormMeta(String formCode){
        for (FormData subData : subData) {
            if (subData.getFormMeta().getCode().equals(formCode)) {
                return subData.getFormMeta();
            }
        }
        return null;
    }


    public static class DataBody{
        private final FormMeta formMeta;
        private final Map<String, Object> data;
        private final Map<String, String> fieldTypes;

        public DataBody(FormMeta formMeta) {
            this.formMeta = formMeta;
            this.data = new HashMap<>();
            this.fieldTypes = formMeta.getMainFieldTypeMaps();
        }

        public DataBody set(String key, Object value) {
            String id = formMeta.getCode() + "." + key;
            String type = this.fieldTypes.get(id);
            if (type == null) {
                throw new RuntimeException("key:" + key + " not found");
            }
            this.data.put(id, value);
            return this;
        }

        public Object get(String key) {
            String id = formMeta.getCode() + "." + key;
            return this.data.get(id);
        }
    }

}

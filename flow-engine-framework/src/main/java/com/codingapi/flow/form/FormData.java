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

    // 当前表单的元数据定义
    private final FormMeta formMeta;
    // 主表单数据内容
    private final DataBody dataBody;
    // 子表单数据内容
    private final List<DataBody> subDataBody;
    // 子表单数据内容
    private final List<FormData> subData;

    public FormData(FormMeta form) {
        this.formMeta = form;
        this.subData = new ArrayList<>();
        this.dataBody = new DataBody(form);
        this.subDataBody = new ArrayList<>();
        if(form.getSubForms()!=null) {
            // 添加子表单
            for (FormMeta subForm : form.getSubForms()) {
                this.subData.add(new FormData(subForm));
            }
        }
    }

    /**
     * 添加子表单数据
     * @param formCode 子表单编号
     */
    public DataBody addSubDataBody(String formCode) {
        FormMeta subFormMeta = getSubFormMeta(formCode);
        if(subFormMeta==null){
            return null;
        }
        DataBody subData = new DataBody(subFormMeta);
        this.subDataBody.add(subData);
        return subData;
    }

    /**
     * 获取子表单数据
     * @param formCode 子表单编号
     */
    public List<DataBody> getSubDataBody(String formCode){
        List<DataBody> list = new ArrayList<>();
        for (DataBody subDataBody : subDataBody) {
            if (subDataBody.isFormCode(formCode)) {
                list.add(subDataBody);
            }
        }
        return list;
    }

    /**
     * 转换成Map数据
     * @return Map数据
     */
    public Map<String,Object> toMapData(){
        return dataBody.toMapData();
    }

    /**
     * 获取子表单元
     * @param formCode 子表单编号
     */
    private FormMeta getSubFormMeta(String formCode){
        for (FormData subData : subData) {
            if (subData.getFormMeta().getCode().equals(formCode)) {
                return subData.getFormMeta();
            }
        }
        return null;
    }

    /**
     * 表单数据体
     */
    public static class DataBody{
        private final FormMeta formMeta;
        private final Map<String, Object> data;
        private final Map<String, String> fieldTypes;

        public DataBody(FormMeta formMeta) {
            this.formMeta = formMeta;
            this.data = new HashMap<>();
            this.fieldTypes = formMeta.getMainFieldTypeMaps();
        }

        /**
         * 判断表单编号
         * @param formCode 表单编号
         * @return true 表单编号一致
         */
        public boolean isFormCode(String formCode) {
            return formMeta.getCode().equals(formCode);
        }

        /**
         * 设置表单字段值
         * @param key 表单字段名称
         * @param value 表单字段值
         */
        public DataBody set(String key, Object value) {
            String id = formMeta.getCode() + "." + key;
            String type = this.fieldTypes.get(id);
            if (type == null) {
                throw new RuntimeException("key:" + key + " not found");
            }
            this.data.put(id, value);
            return this;
        }

        /**
         * 获取表单字段值
         * @param key 表单字段名称
         * @return 表单字段值
         */
        public Object get(String key) {
            String id = formMeta.getCode() + "." + key;
            return this.data.get(id);
        }

        /**
         * 转换成Map数据
         * @return Map数据
         */
        public Map<String, Object> toMapData() {
            Map<String, Object> data = new HashMap<>();
            for (String id : this.data.keySet()) {
                String key = id.substring(id.indexOf(".") + 1);
                Object value = this.data.get(id);
                data.put(key, value);
            }
            return data;
        }
    }

}

package com.codingapi.flow.form;

import lombok.Getter;

import java.util.*;

/**
 * 流程表单数据
 */
public class FormData {

    // 当前表单的元数据定义
    @Getter
    private final FormMeta formMeta;
    // 主表单数据内容
    @Getter
    private final DataBody dataBody;
    // 子表单数据内容
    private final Map<String, List<DataBody>> subDataBody;


    public FormData(FormMeta form) {
        this.formMeta = form;
        this.dataBody = new DataBody(form);
        this.subDataBody = new HashMap<>();
    }

    /**
     * 获取子表单数量
     *
     * @return 子表单数量
     */
    public int countSubDataBody() {
        return subDataBody.size();
    }

    /**
     * 添加子表单数据
     *
     * @param formCode 子表单编号
     */
    public DataBody addSubDataBody(String formCode) {
        FormMeta subFormMeta = getSubFormMeta(formCode);
        if (subFormMeta == null) {
            return null;
        }
        DataBody subData = new DataBody(subFormMeta);
        List<DataBody> list = this.getSubDataBody(formCode);
        if (list == null) {
            list = new ArrayList<>();
            this.subDataBody.put(formCode, list);
        }
        list.add(subData);
        return subData;
    }

    /**
     * 获取子表单数据
     *
     * @param formCode 子表单编号
     */
    public List<DataBody> getSubDataBody(String formCode) {
        return subDataBody.get(formCode);
    }

    /**
     * 重置表单数据
     *
     * @param data 表单数据
     */
    public void reset(Map<String, Object> data) {
        this.dataBody.reset();
        this.subDataBody.clear();

        for (String key : data.keySet()) {
            Object value = data.get(key);
            if (value instanceof Collection<?>) {
                Collection<Map<String, Object>> list = (Collection<Map<String, Object>>) value;
                for (Map<String, Object> item : list) {
                    DataBody body = this.addSubDataBody(key);
                    for (String subKey : item.keySet()) {
                        body.set(subKey, item.get(subKey));
                    }
                }
            } else {
                this.dataBody.set(key, value);
            }
        }
    }

    /**
     * 转换成Map数据
     *
     * @return Map数据
     */
    public Map<String, Object> toMapData() {
        Map<String, Object> map = dataBody.toMapData();
        for (String key : subDataBody.keySet()) {
            List<DataBody> list = subDataBody.get(key);
            List<Map<String, Object>> value = list.stream().map(DataBody::toMapData).toList();
            map.put(key, value);
        }
        return map;
    }

    /**
     * 获取子表单元
     *
     * @param formCode 子表单编号
     */
    private FormMeta getSubFormMeta(String formCode) {
        return formMeta.getSubForm(formCode);
    }

    /**
     * 表单数据体
     */
    public static class DataBody {
        private final FormMeta formMeta;
        private final Map<String, Object> data;
        private final Map<String, String> fieldTypes;

        public DataBody(FormMeta formMeta) {
            this.formMeta = formMeta;
            this.data = new HashMap<>();
            this.fieldTypes = formMeta.loadMainFieldTypeMaps();
        }


        /**
         * 重置表单数据
         */
        public void reset() {
            this.data.clear();
        }

        /**
         * 设置表单字段值
         *
         * @param key   表单字段名称
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
         *
         * @param key 表单字段名称
         * @return 表单字段值
         */
        public Object get(String key) {
            String id = formMeta.getCode() + "." + key;
            return this.data.get(id);
        }

        /**
         * 转换成Map数据
         *
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

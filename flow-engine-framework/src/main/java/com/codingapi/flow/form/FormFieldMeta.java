package com.codingapi.flow.form;

import com.codingapi.flow.utils.RandomUtils;
import lombok.Data;

/**
 * 表单字段元数据
 */
@Data
public class FormFieldMeta {

    // 字段编号
    private String id;
    // 字段名称
    private String name;
    // 字段编号
    private String code;
    // 字段类型
    private String type;
    // 是否必填
    private boolean required;
    // 默认值
    private String defaultValue;

    public FormFieldMeta() {
        this.id = RandomUtils.generateStringId();
    }

}

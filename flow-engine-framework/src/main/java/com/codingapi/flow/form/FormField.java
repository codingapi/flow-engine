package com.codingapi.flow.form;

import com.codingapi.flow.utils.RandomUtils;
import lombok.Data;

/**
 * 表单字段元数据
 */
@Data
public class FormField {

    // 字段编号
    private String id;
    // 字段名称
    private String name;
    // 字段编号
    private String code;
    // 字段类型
    private DataType type;
    // 是否隐藏
    private boolean hidden;
    // 是否必填
    private boolean required;
    // 默认值
    private String defaultValue;
    // 输入提示
    private String placeholder;
    // 提示信息
    private String tooltip;
    // 帮助提示
    private String help;

    public FormField() {
        this.id = RandomUtils.generateStringId();
    }

}

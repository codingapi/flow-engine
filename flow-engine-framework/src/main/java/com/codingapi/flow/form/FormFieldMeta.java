package com.codingapi.flow.form;

import lombok.Data;

/**
 *  表单字段元数据
 */
@Data
public class FormFieldMeta {

    private String name;
    private String code;
    private String type;
    private boolean nullable;
    private String defaultValue;

}

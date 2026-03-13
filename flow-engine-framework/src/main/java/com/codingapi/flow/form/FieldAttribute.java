package com.codingapi.flow.form;

import lombok.Data;

/**
 * 附加属性
 */
@Data
public class FieldAttribute {
    // 属性key
    private String key;
    // 属性名称
    private String label;
    // 属性值
    private String value;
}

package com.codingapi.flow.form;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 流程表单
 */
@Setter
@Getter
public class FlowForm {

    /**
     * 表单名称
     */
    private String name;
    /**
     * 表单编号 唯一标识
     */
    private String code;
    /**
     * 表单字段
     */
    private List<FormFieldMeta> fields;

    /**
     * 子表单
     */
    private List<FlowForm> subForms;
}

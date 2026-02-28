package com.codingapi.flow.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Groovy变量映射DTO
 * 用于前后端变量映射统一
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroovyVariableMapping {

    /**
     * 中文显示名称：如"当前操作人"
     */
    private String label;

    /**
     * 变量展示名：如"request.operatorName"
     */
    private String value;

    /**
     * Groovy表达式：如"request.getOperatorName()"
     */
    private String expression;

    /**
     * 分组标签：如"操作人相关"
     */
    private String tag;

    /**
     * 排序序号
     */
    private Integer order;
}

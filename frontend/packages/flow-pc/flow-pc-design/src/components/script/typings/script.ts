import {DataType} from "@flow-engine/flow-types";

/**
 * Groovy脚本类型枚举
 */
export enum ScriptType {
    /** 标题脚本 */
    TITLE = 'TITLE',
    /** 条件脚本 */
    CONDITION = 'CONDITION',
    /** 人员加载脚本 */
    OPERATOR_LOAD = 'OPERATOR_LOAD',
    /** 流程创建人脚本 */
    OPERATOR_CREATE = 'OPERATOR_LOAD',
    /** 异常触发脚本 */
    ERROR_TRIGGER = 'ERROR_TRIGGER',
    /** 触发节点脚本 */
    TRIGGER = 'TRIGGER',
    /** 路由节点脚本 */
    ROUTER = 'ROUTER',
    /** 子流程节点脚本 */
    SUB_PROCESS = 'SUB_PROCESS',
}

/**
 * Groovy变量映射接口
 * 用于前后端变量映射统一
 */
export interface GroovyVariableMapping {
    /** 中文显示名称：如"当前操作人" */
    label: string;

    /** 变量展示名：如"request.getOperatorName()" */
    value: string;

    /** 数据类型 */
    type:DataType;

    /** Groovy表达式：如"${当前操作人}" */
    expression: string;

    /** 分组标签：如"操作人相关" */
    tag: string;

    /** 排序序号 */
    order: number;
}

/** 变量分组标签枚举 */
export enum VariableTag {
    OPERATOR = '操作人相关',
    WORKFLOW = '流程相关',
    FORM_FIELD = '表单字段',
    WORK_CODE = '流程编号',
}


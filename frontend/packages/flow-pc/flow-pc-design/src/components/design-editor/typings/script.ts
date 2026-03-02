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
  /** 自定义脚本 */
  CUSTOM = 'CUSTOM',
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



/** 自定义脚本标记 */
export const CUSTOM_SCRIPT = '@CUSTOM_SCRIPT';

/** 脚本标题标记 */
export const SCRIPT_TITLE = '@SCRIPT_TITLE';

/**
 * Groovy脚本转换器接口
 */
export interface GroovyScriptConverter {

    /**
     * 转换为完整Groovy脚本
     */
    getScript(): string;

    /**
     * 转换为可视化表达式（回显时）
     */
    toExpression(): string;

    /**
     * 获取默认脚本模板
     */
    getDefaultScript(): string;

    /**
     * 添加变量到脚本中
     * @param variable
     */
    addVariable(variable: GroovyVariableMapping): string;

    /**
     * 重置脚本中的表达式部分（编辑时）
     * @param expression 展示的表达式
     */
    resetExpression(expression: string): void;
}

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


/**
 * Groovy变量适配器接口
 * 用于提供不同脚本类型的变量列表
 */
export interface GroovyVariableAdapter {
    getVariables(): GroovyVariableMapping[];
}

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
 * 脚本适配器接口
 * 用于扩展不同脚本类型的变量和转换逻辑
 */
export interface ScriptAdapter {
  /** 脚本类型 */
  scriptType: ScriptType;

  /** 获取系统变量 */
  getSystemVariables(): import('@flow-engine/flow-types').GroovyVariableMapping[];

  /** 转换为完整Groovy脚本（编辑时） */
  toScript(content: string, mappings: import('@flow-engine/flow-types').GroovyVariableMapping[]): string;

  /** 转换为可视化表达式（回显时） */
  toExpression(script: string, mappings: import('@flow-engine/flow-types').GroovyVariableMapping[]): string | null;

  /** 获取默认脚本模板 */
  getDefaultTemplate(): string;
}

/**
 * 脚本配置项
 */
export interface ScriptConfig {
  /** 脚本类型 */
  type: ScriptType;
  /** 脚本内容 */
  script: string;
  /** 是否为高级模式 */
  advanced?: boolean;
}

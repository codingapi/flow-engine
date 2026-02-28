import { GroovyVariableMapping, VariableTag } from '@flow-engine/flow-types';
import { ScriptType } from '../../typings/groovy-script';

/**
 * Groovy变量预定义映射服务
 */
export class GroovyVariableService {
  /** 适配器映射 */
  private static adapters: Map<ScriptType, any> = new Map();

  /**
   * 注册脚本适配器（供 ScriptAdapterManager 使用）
   */
  static registerAdapter(adapter: any): void {
    this.adapters.set(adapter.scriptType, adapter);
  }

  /**
   * 获取指定脚本类型的系统变量
   */
  static getSystemVariables(scriptType: ScriptType): GroovyVariableMapping[] {
    // 目前所有脚本类型使用相同的预定义变量
    return this.getPredefinedMappings();
  }

  /**
   * 获取变量列表（系统变量 + 表单字段）
   */
  static getVariables(
    scriptType: ScriptType,
    context?: { formFields?: Array<{ name: string; code: string }> }
  ): GroovyVariableMapping[] {
    const mappings = [...this.getPredefinedMappings()];

    if (context?.formFields && context.formFields.length > 0) {
      mappings.push(...this.getFormFieldMappings(context.formFields));
    }

    return mappings;
  }

  /**
   * 获取预定义变量映射
   */
  static getPredefinedMappings(): GroovyVariableMapping[] {
    return [
      // ========== 操作人相关 ==========
      {
        label: '当前操作人',
        value: 'request.operatorName',
        expression: 'request.getOperatorName()',
        tag: VariableTag.OPERATOR,
        order: 1,
      },
      {
        label: '当前操作人ID',
        value: 'request.operatorId',
        expression: 'request.getOperatorId()',
        tag: VariableTag.OPERATOR,
        order: 2,
      },
      {
        label: '是否管理员',
        value: 'request.isFlowManager',
        expression: 'request.getIsFlowManager()',
        tag: VariableTag.OPERATOR,
        order: 3,
      },
      {
        label: '流程创建人',
        value: 'request.creatorName',
        expression: 'request.getCreatorName()',
        tag: VariableTag.OPERATOR,
        order: 4,
      },

      // ========== 流程相关 ==========
      {
        label: '流程标题',
        value: 'request.workflowTitle',
        expression: 'request.getWorkflowTitle()',
        tag: VariableTag.WORKFLOW,
        order: 10,
      },
      {
        label: '流程编码',
        value: 'request.workflowCode',
        expression: 'request.getWorkflowCode()',
        tag: VariableTag.WORKFLOW,
        order: 11,
      },
      {
        label: '当前节点',
        value: 'request.nodeName',
        expression: 'request.getNodeName()',
        tag: VariableTag.WORKFLOW,
        order: 12,
      },
      {
        label: '节点类型',
        value: 'request.nodeType',
        expression: 'request.getNodeType()',
        tag: VariableTag.WORKFLOW,
        order: 13,
      },

      // ========== 流程编号 ==========
      {
        label: '流程编号',
        value: 'request.workCode',
        expression: 'request.getWorkCode()',
        tag: VariableTag.WORK_CODE,
        order: 20,
      },
    ];
  }

  /**
   * 获取表单字段映射（动态生成）
   */
  static getFormFieldMappings(fields: Array<{ name: string; code: string }>): GroovyVariableMapping[] {
    return fields.map((field, index) => ({
      label: field.name,
      value: `request.formData("${field.code}")`,
      expression: `request.getFormData("${field.code}")`,
      tag: VariableTag.FORM_FIELD,
      order: 100 + index,
    }));
  }

  // tag 排序权重（数值越小越靠前）
  private static readonly TAG_ORDER: Record<string, number> = {
    '操作人相关': 1,
    '流程相关': 2,
    '流程编号': 3,
    '表单字段': 4, // 表单字段放在最后
  };

  /**
   * 获取所有变量映射（预定义 + 表单字段）
   */
  static getAllMappings(formFields?: Array<{ name: string; code: string }>): GroovyVariableMapping[] {
    const mappings = [...this.getPredefinedMappings()];

    if (formFields && formFields.length > 0) {
      mappings.push(...this.getFormFieldMappings(formFields));
    }

    // 按tag权重和order排序
    return mappings.sort((a, b) => {
      const tagOrderA = this.TAG_ORDER[a.tag] ?? 99;
      const tagOrderB = this.TAG_ORDER[b.tag] ?? 99;
      if (tagOrderA !== tagOrderB) return tagOrderA - tagOrderB;
      return a.order - b.order;
    });
  }

  /**
   * 按tag分组变量映射
   */
  static groupByTag(mappings: GroovyVariableMapping[]): Map<string, GroovyVariableMapping[]> {
    const groups = new Map<string, GroovyVariableMapping[]>();

    for (const mapping of mappings) {
      const existing = groups.get(mapping.tag) || [];
      existing.push(mapping);
      groups.set(mapping.tag, existing);
    }

    return groups;
  }

  /**
   * 通过label查找映射
   */
  static findByLabel(label: string, mappings: GroovyVariableMapping[]): GroovyVariableMapping | undefined {
    return mappings.find(m => m.label === label);
  }

  /**
   * 检查是否为高级模式（无法解析回可视化）
   * 通过检测完整的 def run(request){// @CUSTOM_SCRIPT ...} 格式
   */
  static isAdvancedMode(script: string): boolean {
    // 检查是否包含 // @CUSTOM_SCRIPT 注释
    if (!script.includes('// @CUSTOM_SCRIPT')) {
      return false;
    }
    // 检查是否有完整的函数包装 def run(request){...}
    return /def\s+run\s*\(\s*request\s*\)\s*\{[\s\S]*\/\/\s*@CUSTOM_SCRIPT[\s\S]*\}/.test(script);
  }
}

/** 单例实例（供适配器使用） */
export const groovyVariableService = new GroovyVariableService();

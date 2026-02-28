import { GroovyVariableMapping, VariableTag } from '@flow-engine/flow-types';
import { ScriptAdapter, ScriptType } from '../../typings/groovy-script';

/**
 * Groovy变量服务类
 * 负责管理脚本变量的获取和映射
 */
export class GroovyVariableService {
  /** 脚本适配器注册表 */
  private adapters: Map<ScriptType, ScriptAdapter> = new Map();

  /**
   * 注册脚本适配器
   */
  public registerAdapter(adapter: ScriptAdapter): void {
    this.adapters.set(adapter.scriptType, adapter);
  }

  /**
   * 获取适配器
   */
  private getAdapter(scriptType: ScriptType): ScriptAdapter | undefined {
    return this.adapters.get(scriptType);
  }

  /**
   * 获取指定脚本类型的变量列表
   * @param scriptType 脚本类型
   * @param context 上下文（可选，包含表单字段等）
   */
  public getVariables(
    scriptType: ScriptType,
    context?: { formFields?: Array<{ name: string; code: string }> }
  ): GroovyVariableMapping[] {
    const adapter = this.getAdapter(scriptType);
    if (adapter) {
      return adapter.getSystemVariables();
    }
    // 默认实现：返回预定义变量
    return this.getDefaultVariables(context);
  }

  /**
   * 获取系统变量（按脚本类型）
   */
  public getSystemVariables(scriptType: ScriptType): GroovyVariableMapping[] {
    const adapter = this.getAdapter(scriptType);
    if (adapter) {
      return adapter.getSystemVariables();
    }
    return this.getPredefinedMappings();
  }

  /**
   * 获取表单字段变量
   */
  public getFormFieldVariables(fields: Array<{ name: string; code: string }>): GroovyVariableMapping[] {
    return fields.map((field, index) => ({
      label: field.name,
      value: `request.formData("${field.code}")`,
      expression: `request.getFormData("${field.code}")`,
      tag: VariableTag.FORM_FIELD,
      order: 100 + index,
    }));
  }

  /**
   * 按tag分组变量映射
   */
  public groupByTag(mappings: GroovyVariableMapping[]): Map<string, GroovyVariableMapping[]> {
    const groups = new Map<string, GroovyVariableMapping[]>();

    for (const mapping of mappings) {
      const existing = groups.get(mapping.tag) || [];
      existing.push(mapping);
      groups.set(mapping.tag, existing);
    }

    return groups;
  }

  /**
   * 默认实现：获取预定义变量映射
   */
  private getPredefinedMappings(): GroovyVariableMapping[] {
    return [
      // 操作人相关
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
      // 流程相关
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
      // 流程编号
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
   * 默认实现：获取所有变量映射
   */
  private getDefaultVariables(context?: { formFields?: Array<{ name: string; code: string }> }): GroovyVariableMapping[] {
    const mappings = [...this.getPredefinedMappings()];

    if (context?.formFields && context.formFields.length > 0) {
      mappings.push(...this.getFormFieldVariables(context.formFields));
    }

    return mappings;
  }
}

/** 单例实例 */
export const groovyVariableService = new GroovyVariableService();

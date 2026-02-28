import { GroovyVariableMapping, VariableTag } from '@flow-engine/flow-types';
import { ScriptAdapter, ScriptType } from '../../typings/groovy-script';
import { GroovySyntaxConverter } from './groovy-syntax-converter';
import { GroovyVariableService } from './groovy-variable-service';

const TITLE_COMMENT = '// @TITLE';
const LEGACY_COMMENT = '// @CUSTOM_SCRIPT';

/**
 * 标题脚本适配器
 */
export class TitleAdapter implements ScriptAdapter {
  public scriptType: ScriptType = ScriptType.TITLE;

  public getSystemVariables(): GroovyVariableMapping[] {
    return [
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
      {
        label: '流程编号',
        value: 'request.workCode',
        expression: 'request.getWorkCode()',
        tag: VariableTag.WORK_CODE,
        order: 20,
      },
    ];
  }

  public toScript(content: string, mappings: GroovyVariableMapping[]): string {
    let result = content;
    const sortedMappings = [...mappings].sort((a, b) => b.label.length - a.label.length);
    const placeholders: Map<string, { expression: string; label: string }> = new Map();
    let placeholderIndex = 0;

    for (const mapping of sortedMappings) {
      const labelPattern = `\${${mapping.label}}`;
      const placeholder = `__PLACEHOLDER_${placeholderIndex}__`;
      placeholders.set(placeholder, { expression: mapping.expression, label: mapping.label });
      result = result.split(labelPattern).join(placeholder);
      placeholderIndex++;
    }

    const parts: string[] = [];
    let lastIndex = 0;
    const placeholderRegex = /__PLACEHOLDER_(\d+)__/g;
    let match;

    while ((match = placeholderRegex.exec(result)) !== null) {
      if (match.index > lastIndex) {
        const text = result.substring(lastIndex, match.index);
        parts.push(`"${text}"`);
      }
      const placeholder = match[0];
      const placeholderInfo = placeholders.get(placeholder);
      if (placeholderInfo) {
        parts.push(placeholderInfo.expression);
      }
      lastIndex = match.index + placeholder.length;
    }

    if (lastIndex < result.length) {
      const text = result.substring(lastIndex);
      parts.push(`"${text}"`);
    }

    let groovyExpression: string;
    if (parts.length === 0) {
      groovyExpression = '""';
    } else if (parts.length === 1) {
      groovyExpression = parts[0];
    } else {
      groovyExpression = parts.join(' + ');
    }

    return `def run(request){\n${TITLE_COMMENT}\nreturn ${groovyExpression}\n}`;
  }

  public toExpression(script: string, mappings: GroovyVariableMapping[]): string | null {
    try {
      let result = script.trim();

      // 特殊处理：检查是否为默认的 legacy 脚本
      if (this.isDefaultLegacyScript(result)) {
        const match = result.match(/return\s+['"]([^'"]*)['"]/);
        if (match && match[1]) {
          return match[1];
        }
      }

      // 支持两种标记：// @TITLE 和 // @CUSTOM_SCRIPT
      if (!result.includes(TITLE_COMMENT) && !result.includes(LEGACY_COMMENT)) {
        return null;
      }

      const funcMatch = result.match(/def\s+run\s*\([^)]*\)\s*\{([\s\S]*)\}/);
      if (funcMatch) {
        result = funcMatch[1];
      }

      // 移除两种标记
      result = result.replace(TITLE_COMMENT, '').replace(LEGACY_COMMENT, '').trim();
      result = result.replace(/^return\s+/, '');

      const exprToLabel = new Map<string, string>();
      for (const mapping of mappings) {
        exprToLabel.set(mapping.expression, mapping.label);
      }

      const sortedExprs = Array.from(exprToLabel.keys()).sort((a, b) => b.length - a.length);
      let placeholders = 0;
      const placeholderMap = new Map<string, string>();

      for (const expr of sortedExprs) {
        const escaped = expr.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        const placeholder = `___EXPR_PLACEHOLDER_${placeholders}___`;
        result = result.replace(new RegExp(escaped, 'g'), placeholder);
        placeholderMap.set(placeholder, exprToLabel.get(expr) || expr);
        placeholders++;
      }

      result = result.replace(/\s*\+\s*"/g, '"');
      result = result.replace(/"\s*\+\s*/g, '"');
      result = result.replace(/"/g, '');

      placeholderMap.forEach((label, placeholder) => {
        result = result.replace(new RegExp(placeholder.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g'), `\${${label}}`);
      });

      result = result.replace(/\$\{([^}]+)\}\s*\+\s*\$\{/g, '\$\{$1\}\$\{');

      return result;
    } catch (e) {
      return null;
    }
  }

  public getDefaultTemplate(): string {
    return "def run(request){\n// @TITLE\nreturn '你有一条待办'\n}";
  }

  private isDefaultLegacyScript(script: string): boolean {
    const trimmed = script.trim();
    return /^\s*def\s+run\s*\(\s*request\s*\)\s*\{\s*return\s+'[^']*'\s*\}\s*$/.test(trimmed);
  }
}

/**
 * 初始化所有脚本适配器
 */
export class ScriptAdapterManager {
  private syntaxConverter: GroovySyntaxConverter;
  private variableService: GroovyVariableService;

  constructor(
    syntaxConverter: GroovySyntaxConverter,
    variableService: GroovyVariableService
  ) {
    this.syntaxConverter = syntaxConverter;
    this.variableService = variableService;
  }

  public initialize(): void {
    const titleAdapter = new TitleAdapter();
    this.syntaxConverter.registerAdapter(titleAdapter);
    this.variableService.registerAdapter(titleAdapter);
  }
}

// 使用已导出的单例实例
import { groovySyntaxConverter } from './groovy-syntax-converter';
import { groovyVariableService } from './groovy-variable-service';

export const scriptAdapterManager = new ScriptAdapterManager(
  groovySyntaxConverter,
  groovyVariableService
);

// 自动初始化适配器 - 确保在任何地方使用服务前完成初始化
scriptAdapterManager.initialize();

import { GroovyVariableMapping } from '@flow-engine/flow-types';
import { ScriptAdapter, ScriptType } from '../../typings/groovy-script';

/** 自定义注释标记 */
const CUSTOM_COMMENT = '// @CUSTOM_SCRIPT';

/**
 * Groovy语法转换服务类
 * 负责脚本语法与可视化表达式之间的转换
 */
export class GroovySyntaxConverter {
  /** 脚本适配器注册表 */
  private adapters: Map<ScriptType, ScriptAdapter> = new Map();

  /** 是否已初始化 */
  private initialized = false;

  /**
   * 注册脚本适配器
   */
  public registerAdapter(adapter: ScriptAdapter): void {
    this.adapters.set(adapter.scriptType, adapter);
  }

  /**
   * 获取适配器（带自动初始化）
   */
  private getAdapter(scriptType: ScriptType): ScriptAdapter | undefined {
    // 尝试自动初始化（仅一次）
    if (!this.initialized) {
      this.autoInitialize();
      this.initialized = true;
    }
    return this.adapters.get(scriptType);
  }

  /**
   * 自动初始化适配器
   */
  private autoInitialize(): void {
    // 动态导入 TitleAdapter 进行初始化
    import('./adapters').then(({ TitleAdapter }) => {
      if (!this.adapters.has(ScriptType.TITLE)) {
        this.adapters.set(ScriptType.TITLE, new TitleAdapter());
      }
    }).catch(() => {
      // 忽略初始化错误
    });
  }

  /**
   * 转换为完整Groovy脚本
   * @param scriptType 脚本类型
   * @param content 用户输入的可视化表达式内容
   * @param mappings 变量映射列表
   */
  public toScript(
    scriptType: ScriptType,
    content: string,
    mappings: GroovyVariableMapping[]
  ): string {
    const adapter = this.getAdapter(scriptType);
    if (adapter) {
      return adapter.toScript(content, mappings);
    }
    // 默认实现
    return this.convertToGroovySyntax(content, mappings, CUSTOM_COMMENT);
  }

  /**
   * 转换为可视化表达式（回显时）
   * @param scriptType 脚本类型
   * @param script 后端返回的完整Groovy脚本
   * @param mappings 变量映射列表
   */
  public toExpression(
    scriptType: ScriptType,
    script: string,
    mappings: GroovyVariableMapping[]
  ): string | null {
    const adapter = this.getAdapter(scriptType);
    if (adapter) {
      return adapter.toExpression(script, mappings);
    }
    // 默认实现
    return this.convertToLabelExpression(script, mappings, CUSTOM_COMMENT);
  }

  /**
   * 获取默认脚本模板
   */
  public getDefaultTemplate(scriptType: ScriptType): string {
    const adapter = this.getAdapter(scriptType);
    if (adapter) {
      return adapter.getDefaultTemplate();
    }
    // 默认模板
    return 'def run(request){\nreturn ""\n}';
  }

  /**
   * 检查是否为高级模式
   * 高级模式：包含 // @CUSTOM_SCRIPT 标记的自定义脚本
   * 普通模式：包含 // @TITLE 或其他特定标记的脚本
   */
  public isAdvancedMode(script: string): boolean {
    // 触发自动初始化
    this.getAdapter(ScriptType.TITLE);

    // 如果包含 @CUSTOM_SCRIPT 标记，则是高级模式
    if (script.includes(CUSTOM_COMMENT)) {
      return true;
    }
    return false;
  }

  /**
   * 通用实现：将可视化表达式转换为Groovy语法
   */
  private convertToGroovySyntax(
    content: string,
    mappings: GroovyVariableMapping[],
    comment: string
  ): string {
    let result = content;

    // 按label长度降序排序，避免短label替换长label的一部分
    const sortedMappings = [...mappings].sort((a, b) => b.label.length - a.label.length);

    // 将 ${label} 替换为唯一的占位符
    const placeholders: Map<string, { expression: string; label: string }> = new Map();
    let placeholderIndex = 0;
    for (const mapping of sortedMappings) {
      const labelPattern = `\${${mapping.label}}`;
      const placeholder = `__PLACEHOLDER_${placeholderIndex}__`;
      placeholders.set(placeholder, { expression: mapping.expression, label: mapping.label });
      result = result.split(labelPattern).join(placeholder);
      placeholderIndex++;
    }

    // 按占位符分割字符串，构建表达式
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

    return `def run(request){\n${comment}\nreturn ${groovyExpression}\n}`;
  }

  /**
   * 通用实现：将Groovy脚本转换为可视化表达式
   */
  private convertToLabelExpression(
    groovyCode: string,
    mappings: GroovyVariableMapping[],
    comment: string
  ): string | null {
    try {
      let result = groovyCode.trim();

      // 检查是否有自定义注释
      if (!result.includes(comment)) {
        return null;
      }

      // 移除函数包装
      const funcMatch = result.match(/def\s+run\s*\([^)]*\)\s*\{([\s\S]*)\}/);
      if (funcMatch) {
        result = funcMatch[1];
      }

      // 移除注释
      result = result.replace(comment, '').trim();

      // 移除 return 关键字
      result = result.replace(/^return\s+/, '');

      // 创建表达式到标签的映射
      const exprToLabel = new Map<string, string>();
      for (const mapping of mappings) {
        exprToLabel.set(mapping.expression, mapping.label);
      }

      // 按expression长度降序排序
      const sortedExprs = Array.from(exprToLabel.keys()).sort((a, b) => b.length - a.length);

      // 使用占位符方法处理表达式替换
      let placeholders = 0;
      const placeholderMap = new Map<string, string>();

      for (const expr of sortedExprs) {
        const escaped = this.escapeRegex(expr);
        const placeholder = `___EXPR_PLACEHOLDER_${placeholders}___`;

        result = result.replace(new RegExp(escaped, 'g'), placeholder);
        placeholderMap.set(placeholder, exprToLabel.get(expr) || expr);
        placeholders++;
      }

      // 处理字符串拼接
      result = result.replace(/\s*\+\s*"/g, '"');
      result = result.replace(/"\s*\+\s*/g, '"');

      // 移除所有剩余的引号
      result = result.replace(/"/g, '');

      // 还原占位符为 ${label}
      placeholderMap.forEach((label, placeholder) => {
        result = result.replace(new RegExp(this.escapeRegex(placeholder), 'g'), `\${${label}}`);
      });

      // 处理连续变量之间剩余的 + 号
      result = result.replace(/\$\{([^}]+)\}\s*\+\s*\$\{/g, '\$\{$1\}\$\{');

      return result;
    } catch (e) {
      console.error('Failed to parse label expression:', e);
      return null;
    }
  }

  /**
   * 转义正则表达式特殊字符
   */
  private escapeRegex(str: string): string {
    return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }
}

/** 单例实例 */
export const groovySyntaxConverter = new GroovySyntaxConverter();

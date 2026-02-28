import { GroovyVariableMapping } from '@flow-engine/flow-types';
import { GroovyVariableService } from './groovy-variable-service';

const TITLE_COMMENT = '// @CUSTOM_SCRIPT';

/**
 * 标题表达式语法转换工具
 */
export class TitleSyntaxConverter {
  /**
   * 用户界面显示语法 → Groovy脚本语法
   * 输入: "你好，${当前操作人}"
   * 输出: "// @TITLE\nreturn \"你好，\" + request.getOperatorName()"
   */
  static toGroovySyntax(
    content: string,
    mappings: GroovyVariableMapping[]
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
      // 添加占位符之前的文本
      if (match.index > lastIndex) {
        const text = result.substring(lastIndex, match.index);
        parts.push(`"${text}"`);
      }
      // 添加占位符对应的表达式
      const placeholder = match[0];
      const placeholderInfo = placeholders.get(placeholder);
      if (placeholderInfo) {
        parts.push(placeholderInfo.expression);
      }
      lastIndex = match.index + placeholder.length;
    }

    // 添加最后一个占位符之后的文本
    if (lastIndex < result.length) {
      const text = result.substring(lastIndex);
      parts.push(`"${text}"`);
    }

    // 组合成完整的Groovy表达式
    let groovyExpression: string;
    if (parts.length === 0) {
      groovyExpression = '""';
    } else if (parts.length === 1) {
      groovyExpression = parts[0];
    } else {
      groovyExpression = parts.join(' + ');
    }

    // 添加 return 和标题注释，包装成完整的Groovy函数
    // 后端期望格式: def run(request){// @TITLE\nreturn "..."}
    return `def run(request){\n${TITLE_COMMENT}\nreturn ${groovyExpression}\n}`;
  }

  /**
   * Groovy脚本语法 → 用户界面显示语法
   * 输入: "// @TITLE\nreturn \"你好，\" + request.getOperatorName()"
   * 输出: "你好，${当前操作人}"
   */
  static toLabelExpression(
    groovyCode: string,
    mappings: GroovyVariableMapping[]
  ): string | null {
    try {
      let result = groovyCode.trim();

      // 特殊处理：检查是否为默认的 legacy 脚本
      // 格式: def run(request){return '你有一条待办'}
      if (this.isDefaultLegacyScript(result)) {
        // 提取 return 语句中的字符串
        const match = result.match(/return\s+['"]([^'"]*)['"]/);
        if (match && match[1]) {
          return match[1];
        }
      }

      // 检查是否有 @TITLE 注释
      if (!result.includes(TITLE_COMMENT)) {
        return null; // 无法解析
      }

      // 移除函数包装 def run(request){...}
      // 提取函数体内容
      const funcMatch = result.match(/def\s+run\s*\([^)]*\)\s*\{([\s\S]*)\}/);
      if (funcMatch) {
        result = funcMatch[1];
      }

      // 移除 @TITLE 注释
      result = result.replace(TITLE_COMMENT, '').trim();

      // 移除 return 关键字
      result = result.replace(/^return\s+/, '');

      // 创建表达式到标签的映射
      const exprToLabel = new Map<string, string>();
      for (const mapping of mappings) {
        exprToLabel.set(mapping.expression, mapping.label);
      }

      // 按expression长度降序排序（优先匹配长表达式）
      const sortedExprs = Array.from(exprToLabel.keys()).sort((a, b) => b.length - a.length);

      // 使用占位符方法处理表达式替换
      let placeholders = 0;
      const placeholderMap = new Map<string, string>();

      // 首先替换所有表达式为占位符
      for (const expr of sortedExprs) {
        const escaped = this.escapeRegex(expr);
        const placeholder = `___EXPR_PLACEHOLDER_${placeholders}___`;

        result = result.replace(new RegExp(escaped, 'g'), placeholder);
        placeholderMap.set(placeholder, exprToLabel.get(expr) || expr);
        placeholders++;
      }

      // 处理字符串拼接：移除 + 连接符
      // 情况1：expr + "text" -> expr text（删除 " 前面的 + 和空格）
      result = result.replace(/\s*\+\s*"/g, '"');
      // 情况2："text" + expr -> text expr（删除 " 后面的 + 和空格）
      result = result.replace(/"\s*\+\s*/g, '"');

      // 移除所有剩余的引号
      result = result.replace(/"/g, '');

      // 还原占位符为 ${label}
      placeholderMap.forEach((label, placeholder) => {
        result = result.replace(new RegExp(this.escapeRegex(placeholder), 'g'), `\${${label}}`);
      });

      // 处理连续变量之间剩余的 + 号（如 ${变量1} + ${变量2} -> ${变量1}${变量2}）
      // 先把 ${xxx} 替换为占位符，处理中间的 + 号，再还原
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
  private static escapeRegex(str: string): string {
    return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }

  /**
   * 解析脚本模式
   */
  static parseMode(script: string): 'normal' | 'advanced' {
    // 特殊处理：检测是否为默认的 legacy 脚本
    // 如果是默认脚本，视为 normal 模式（可以重新编辑）
    if (this.isDefaultLegacyScript(script)) {
      return 'normal';
    }

    if (GroovyVariableService.isAdvancedMode(script)) {
      // 尝试解析为正常模式
      const mappings = GroovyVariableService.getPredefinedMappings();
      const labelExpr = this.toLabelExpression(script, mappings);
      return labelExpr !== null ? 'normal' : 'advanced';
    }
    // 旧脚本或无注释脚本，视为高级模式
    return 'advanced';
  }

  /**
   * 检查是否为默认的 legacy 脚本
   */
  private static isDefaultLegacyScript(script: string): boolean {
    const trimmed = script.trim();
    // 匹配默认脚本格式: def run(request){return '你有一条待办'}
    return /^\s*def\s+run\s*\(\s*request\s*\)\s*\{\s*return\s+'[^']*'\s*\}\s*$/.test(trimmed);
  }

  /**
   * 验证Groovy脚本语法（基础检查）
   */
  static validateGroovySyntax(script: string): { valid: boolean; error?: string } {
    if (!script || script.trim().length === 0) {
      return { valid: false, error: '脚本不能为空' };
    }

    // 基础语法检查
    if (!script.includes('return')) {
      return { valid: false, error: '脚本必须包含 return 语句' };
    }

    // 检查括号匹配
    const openBraces = (script.match(/\{/g) || []).length;
    const closeBraces = (script.match(/\}/g) || []).length;
    if (openBraces !== closeBraces) {
      return { valid: false, error: '大括号不匹配' };
    }

    const openParens = (script.match(/\(/g) || []).length;
    const closeParens = (script.match(/\)/g) || []).length;
    if (openParens !== closeParens) {
      return { valid: false, error: '圆括号不匹配' };
    }

    return { valid: true };
  }
}

import { GroovyVariableMapping } from '@flow-engine/flow-types';
import { GroovyVariableService } from '@/services/groovy-variable-service';

const TITLE_COMMENT = '// @TITLE';

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

    // 添加 return 和标题注释
    return `${TITLE_COMMENT}\nreturn ${groovyExpression}`;
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

      // 检查是否有 @TITLE 注释
      if (!result.includes(TITLE_COMMENT)) {
        return null; // 无法解析
      }

      // 移除 @TITLE 注释
      result = result.replace(TITLE_COMMENT, '').trim();

      // 移除 return 关键字
      result = result.replace(/^return\s+/, '');

      // 按expression长度降序排序
      const sortedMappings = [...mappings].sort(
        (a, b) => b.expression.length - a.expression.length
      );

      // 将 expression 替换为 ${label}
      for (const mapping of sortedMappings) {
        const escapedExpr = this.escapeRegex(mapping.expression);

        // 处理各种模式
        // 1. "text" + expression + "text" (最常见)
        result = result.replace(
          new RegExp(`"([^"]*)"\\s*\\+\\s*${escapedExpr}\\s*\\+\\s*"([^"]*)"`, 'g'),
          (match, before, after) => {
            return `${before}\${${mapping.label}}${after}`;
          }
        );

        // 2. expression + "text"
        result = result.replace(
          new RegExp(`^${escapedExpr}\\s*\\+\\s*"([^"]*)"$`, 'g'),
          (_, after) => `\${${mapping.label}}${after}`
        );

        // 3. "text" + expression
        result = result.replace(
          new RegExp(`^"([^"]*)"\\s*\\+\\s*${escapedExpr}$`, 'g'),
          (_, before) => `${before}\${${mapping.label}}`
        );

        // 4. 单独的 expression
        result = result.replace(
          new RegExp(`^${escapedExpr}$`, 'g'),
          `\${${mapping.label}}`
        );

        // 5. " + expression + " (空字符串)
        result = result.replace(
          new RegExp(`"\\s*\\+\\s*${escapedExpr}\\s*\\+\\s*"`, 'g'),
          `\${${mapping.label}}`
        );
      }

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

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

    // 将 ${label} 替换为 " + expression + "
    for (const mapping of sortedMappings) {
      const labelPattern = `\${${mapping.label}}`;
      result = result.split(labelPattern).join(`" + ${mapping.expression} + "`);
    }

    // 清理多余的空字符串拼接: "" +  或  + ""
    result = result.replace(/"\s*\+\s*/g, '');
    result = result.replace(/\s*\+\s*"/g, '');

    // 添加 return 和引号
    result = `return "${result}"`;

    // 添加标题注释
    return `${TITLE_COMMENT}\n${result}`;
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

      // 提取 return "..." 中的内容
      const returnMatch = result.match(/return\s+"([^"]*(?:\\"[^"]*)*)"/);
      if (!returnMatch) {
        return null; // 无法解析
      }

      result = returnMatch[1];

      // 替换转义字符
      result = result.replace(/\\"/g, '"');

      // 按expression长度降序排序
      const sortedMappings = [...mappings].sort(
        (a, b) => b.expression.length - a.expression.length
      );

      // 将 expression 替换为 ${label}
      for (const mapping of sortedMappings) {
        // 匹配 " + expression + " 或 expression + " 或 " + expression
        const patterns = [
          new RegExp(`"\\s*\\+\\s*${this.escapeRegex(mapping.expression)}\\s*\\+\\s*"`, 'g'),
          new RegExp(`${this.escapeRegex(mapping.expression)}\\s*\\+\\s*"`, 'g'),
          new RegExp(`"\\s*\\+\\s*${this.escapeRegex(mapping.expression)}`, 'g'),
        ];

        for (const pattern of patterns) {
          result = result.replace(pattern, `\${${mapping.label}}`);
        }
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

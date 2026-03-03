import {CUSTOM_SCRIPT,SCRIPT_TITLE,GroovyVariableMapping} from "@/components/script/typings";
import {GroovyFormatter} from "@/components/script/utils/format";

/**
 * Groovy脚本转换器工具类，提供一些通用的脚本处理方法
 */
export class GroovyScriptConvertorUtil {

    /**
     * 判断脚本是否包含自定义注释标记
     * @param script
     */
    public static isCustomScript(script: string): boolean {
        return script.includes(CUSTOM_SCRIPT);
    }


    /**
     *  格式化脚本内容，去除多余空白等
     *  @param script
     */
    public static formatScript(script: string): string {
        // 这里可以添加一些格式化逻辑，比如统一换行、缩进等
        return GroovyFormatter.formatScript(script);
    }


    /**
     *  将普通脚本转换为包含自定义注释标记的脚本
     * @param script
     */
    public static toCustomScript(script: string): string {
        if (GroovyScriptConvertorUtil.isCustomScript(script)) {
            return GroovyFormatter.formatScript(script);
        }
        return GroovyFormatter.formatScript(`// ${CUSTOM_SCRIPT}\n${script}`);
    }


    /**
     * 获取脚本中的标题注释内容
     * @param script
     */
    public static getScriptTitle(script: string): string {
        const titleMatch = script.match(new RegExp(`//\\s*${SCRIPT_TITLE}\\s*(.+)`));
        if (titleMatch) {
            return titleMatch[1].trim();
        }
        return '';
    }

    /**
     * 更新脚本中的标题注释内容，如果不存在则添加
     * @param script
     * @param title
     */
    public static updateScriptTitle(script: string, title: string): string {
        const titleComment = `// ${SCRIPT_TITLE} ${title}`;
        if (GroovyScriptConvertorUtil.getScriptTitle(script)) {
            return script.replace(new RegExp(`//\\s*${SCRIPT_TITLE}\\s*.+`), titleComment);
        } else {
            return `${titleComment}\n${script}`;
        }
    }


    /**
     * 清除脚本中的注释
     * @param script
     */
    public static clearComments(script: string): string {
        return script.replace(/\/\/.*$/gm, '').trim();
    }

    /**
     * 提取脚本中的return表达式
     * @param script
     */
    public static getReturnScript(script: string): string {
        try {
            let result = GroovyScriptConvertorUtil.clearComments(script);
            const funcMatch = result.match(/def\s+run\s*\([^)]*\)\s*\{([\s\S]*)\}/);
            if (funcMatch) {
                result = funcMatch[1];
            }
            const returnMatch = result.match(/return\s+(.+?);?\s*$/m);
            if (returnMatch) {
                result = returnMatch[1].trim();
            } else {
                // 如果没有找到return语句，可能整个脚本就是表达式
                result = result.trim();
            }

            return result;
        } catch (e) {
            return '';
        }
    }

    /**
     * 将可视化表达式转换为Groovy表达式（编辑时）
     * @param expression
     * @param variables
     */
    public static toScript(expression: string, variables: GroovyVariableMapping[]): string {

        let result = expression;

        // 按label长度降序排序，避免短label替换长label的一部分
        const sortedMappings = [...variables].sort((a, b) => b.label.length - a.label.length);

        // 将 ${label} 替换为唯一的占位符
        const placeholders: Map<string, string> = new Map();
        let placeholderIndex = 0;
        for (const mapping of sortedMappings) {
            const labelPattern = `\${${mapping.label}}`;
            const placeholder = `__PLACEHOLDER_${placeholderIndex}__`;
            placeholders.set(placeholder, mapping.value);
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
                parts.push(placeholderInfo);
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

        return groovyExpression;
    }


    /**
     * 将Groovy表达式转换为可视化表达式（回显时）
     * @param returnScript
     * @param variables
     */
    public static toExpression(returnScript: string, variables: GroovyVariableMapping[]): string {
        let result = returnScript;
        const exprToLabel = new Map<string, string>();
        for (const mapping of variables) {
            exprToLabel.set(mapping.value, mapping.label);
        }

        const sortedExpress = Array.from(exprToLabel.keys()).sort((a, b) => b.length - a.length);
        let placeholders = 0;
        const placeholderMap = new Map<string, string>();

        for (const expr of sortedExpress) {
            const escaped = expr.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
            const placeholder = `___EXPR_PLACEHOLDER_${placeholders}___`;
            result = result.replace(new RegExp(escaped, 'g'), placeholder);
            placeholderMap.set(placeholder, exprToLabel.get(expr) || expr);
            placeholders++;
        }

        result = result.replace(/\s*\+\s*"/g, '"');
        result = result.replace(/"\s*\+\s*/g, '"');
        result = result.replace(/"/g, '');

        // 在替换占位符之前，先移除占位符之间的 + 符号
        result = result.replace(/___\w+___\s*\+\s*___\w+___/g, (match) => {
            return match.replace(/\s*\+\s*/g, '');
        });

        placeholderMap.forEach((label, placeholder) => {
            result = result.replace(new RegExp(placeholder.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g'), `\${${label}}`);
        });

        result = result.replace(/\$\{([^}]+)\}\s*\+\s*\$\{/g, '\$\{$1\}\$\{');
        return result;
    }

}
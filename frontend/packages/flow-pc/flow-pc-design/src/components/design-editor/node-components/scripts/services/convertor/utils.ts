import {GroovyVariableMapping, ScriptType} from "@/components/design-editor/typings/script";
import {
    NodeTitleGroovyConvertor
} from "@/components/design-editor/node-components/scripts/services/convertor/node-title";

/** 自定义脚本标记 */
const CUSTOM_SCRIPT = '@CUSTOM_SCRIPT';

/** 脚本标题标记 */
const SCRIPT_TITLE = '@SCRIPT_TITLE';

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
 * Groovy脚本转换器构造函数类型
 */
export type GroovyScriptConverterConstructor = new (script: string, variables: GroovyVariableMapping[]) => GroovyScriptConverter;


/**
 * Groovy脚本转换器上下文，负责管理不同类型脚本的转换器
 */
export class GroovyScriptConverterContext {
    private converterMap: Map<ScriptType, GroovyScriptConverterConstructor>;

    private register(scriptType: ScriptType, converter: GroovyScriptConverterConstructor) {
        this.converterMap.set(scriptType, converter);
    }

    public createConverter(scriptType: ScriptType, script: string, variables: GroovyVariableMapping[]): GroovyScriptConverter | undefined {
        const constructor = this.converterMap.get(scriptType);
        if (constructor) {
            return new constructor(script, variables);
        }
        return undefined;
    }

    private constructor() {
        this.converterMap = new Map();
        this.initializeDefaultConverters();
    }

    private initializeDefaultConverters() {
        try {
            this.register(ScriptType.TITLE, NodeTitleGroovyConvertor);
        } catch (e) {
            console.error('Failed to register default converters:', e);
        }
    }

    private static readonly instance = new GroovyScriptConverterContext();

    public static getInstance(): GroovyScriptConverterContext {
        return GroovyScriptConverterContext.instance;
    }
}

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
     *  将普通脚本转换为包含自定义注释标记的脚本
     * @param script
     */
    public static toCustomScript(script: string): string {
        if (GroovyScriptConvertorUtil.isCustomScript(script)) {
            return script;
        }
        return `// ${CUSTOM_SCRIPT}\n${script}`;
    }


    /**
     * 获取脚本中的标题注释内容
     * @param script
     */
    public static getScriptTitle(script: string): string {
        const titleMatch = script.match(new RegExp(`//\\s*${SCRIPT_TITLE}:\\s*(.+)`));
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
        const titleComment = `// ${SCRIPT_TITLE}: ${title}`;
        if (GroovyScriptConvertorUtil.getScriptTitle(script)) {
            return script.replace(new RegExp(`//\\s*${SCRIPT_TITLE}:\\s*.+`), titleComment);
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
     * @param script
     * @param variables
     */
    public static toExpression(script: string, variables: GroovyVariableMapping[]): string {
        let result = script;
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
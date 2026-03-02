import {GroovyVariableMapping} from "@/components/design-editor/typings/script";

/** 自定义注释标记 */
const CUSTOM_COMMENT = '@CUSTOM_SCRIPT';


/**
 * Groovy脚本转换器接口
 */
export interface GroovyScriptConverter {

    /**
     * 转换为完整Groovy脚本（编辑时）
     */
    toScript():string;

    /**
     * 转换为可视化表达式（回显时）
     */
    toExpression():string;

    /**
     * 获取默认脚本模板
     */
    getDefaultScript():string;

    /**
     * 添加变量到脚本中
     * @param variable
     */
    addVariable(variable: GroovyVariableMapping): string;

    /**
     * 重置脚本中的表达式部分（编辑时）
     * @param value
     */
    resetExpression(value:string):void;
}


export class GroovyScriptUtil {

    /**
     * 判断脚本是否包含自定义注释标记
     * @param script
     */
    public static isCustomScript(script: string): boolean {
        return script.includes(CUSTOM_COMMENT);
    }

    /**
     *  将普通脚本转换为包含自定义注释标记的脚本
     * @param script
     */
    public static toCustomScript(script: string): string {
        if (GroovyScriptUtil.isCustomScript(script)) {
            return script;
        }
        return `// ${CUSTOM_COMMENT}\n${script}`;
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
    public static getReturnExpression(script: string): string {
        try {
            let result = GroovyScriptUtil.clearComments(script);
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
     * 将Groovy表达式转换为可视化表达式（回显时）
     * @param script
     * @param mappings
     */
    public static toExpression(script: string, mappings: GroovyVariableMapping[]): string {
        let result = script;
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
    }

}
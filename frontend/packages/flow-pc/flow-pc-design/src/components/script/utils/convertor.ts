import {CUSTOM_SCRIPT, SCRIPT_META, SCRIPT_TITLE} from "@/components/script/typings";
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
     * 获取脚本中的元数据
     * @param script
     */
    public static getScriptMeta(script: string): string {
        const titleMatch = script.match(new RegExp(`//\\s*${SCRIPT_META}\\s*(.+)`));
        if (titleMatch) {
            return titleMatch[1].trim();
        }
        return '';
    }


    /**
     * 更新脚本中的元数据内容，如果不存在则添加
     * @param script
     * @param meta
     */
    public static updateScriptMeta(script: string, meta: string): string {
        const metaComment = `// ${SCRIPT_META} ${meta}`;
        if (GroovyScriptConvertorUtil.getScriptTitle(script)) {
            return script.replace(new RegExp(`//\\s*${SCRIPT_META}\\s*.+`), metaComment);
        } else {
            return `${metaComment}\n${script}`;
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

}
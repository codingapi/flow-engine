import {GroovyVariableMapping, VariableTag} from "@/components/script/typings";
import {FlowForm} from "@flow-engine/flow-types";

export class GroovyVariableUtil {

     public static getDefaultVariables(): GroovyVariableMapping[] {
        return [
            // ========== 操作人相关 ==========
            {
                label: '当前操作人',
                value: 'request.getOperatorName()',
                expression: "${当前操作人}",
                tag: VariableTag.OPERATOR,
                type:'STRING',
                order: 1,
            },
            {
                label: '当前操作人ID',
                value: 'request.getOperatorId()',
                expression: '${当前操作人ID}',
                tag: VariableTag.OPERATOR,
                type:'NUMBER',
                order: 2,
            },
            {
                label: '是否管理员',
                value: 'request.getIsFlowManager()',
                expression: '${是否管理员}',
                type:'BOOLEAN',
                tag: VariableTag.OPERATOR,
                order: 3,
            },
            {
                label: '流程创建人',
                value: 'request.getCreatorName()',
                expression: '${流程创建人}',
                tag: VariableTag.OPERATOR,
                type:'STRING',
                order: 4,
            },

            // ========== 流程相关 ==========
            {
                label: '流程标题',
                value: 'request.getWorkflowTitle()',
                expression: '${流程标题}',
                tag: VariableTag.WORKFLOW,
                type:'STRING',
                order: 10,
            },
            {
                label: '流程编码',
                value: 'request.getWorkflowCode()',
                expression: '${流程编码}',
                tag: VariableTag.WORKFLOW,
                type:'STRING',
                order: 11,
            },
            {
                label: '当前节点',
                value: 'request.getNodeName()',
                expression: '${当前节点}',
                tag: VariableTag.WORKFLOW,
                type:'STRING',
                order: 12,
            },
            {
                label: '节点类型',
                value: 'request.getNodeType()',
                expression: '${节点类型}',
                tag: VariableTag.WORKFLOW,
                type:'STRING',
                order: 13,
            },

            // ========== 流程编号 ==========
            {
                label: '流程编号',
                value: 'request.getWorkCode()',
                expression: '${流程编号}',
                tag: VariableTag.WORK_CODE,
                type:'STRING',
                order: 20,
            }
        ];
    }

    /**
     * 流程表单参数
     * @param flowForm
     */
    public static getMainFormMetaVariables(flowForm: FlowForm): GroovyVariableMapping[] {
        if (!flowForm || !flowForm.fields) {
            return [];
        }

        return flowForm.fields.map((field, index) => ({
            label: `${field.name}`,
            value: `request.getFormData('${field.code}')`,
            expression: "${" + `${field.name}` + "}",
            type:field.type,
            tag: VariableTag.FORM_FIELD,
            order: 100 + index,
        }));
    }

    /**
     * 获取全部表单参数
     * @param form
     */
    public static getVariables(form:FlowForm): GroovyVariableMapping[] {
        const variables: GroovyVariableMapping[] = [];
        // 添加默认变量
        variables.push(...GroovyVariableUtil.getDefaultVariables());
        // 添加表单字段变量
        variables.push(...GroovyVariableUtil.getMainFormMetaVariables(form));
        return variables;
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
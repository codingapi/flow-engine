import {GroovyVariableMapping, VariableTag} from "@/components/design-editor/typings/script";
import {FlowFromMeta} from "@flow-engine/flow-types";

/**
 * Groovy变量适配器接口
 * 用于提供不同脚本类型的变量列表
 */
export interface GroovyVariableAdapter {
    getVariables(): GroovyVariableMapping[];
}


export class GroovyVariableUtil {

    getDefaultVariables(): GroovyVariableMapping[] {
        return [
            // ========== 操作人相关 ==========
            {
                label: '当前操作人',
                value: 'request.getOperatorName()',
                expression: "${当前操作人}",
                tag: VariableTag.OPERATOR,
                order: 1,
            },
            {
                label: '当前操作人ID',
                value: 'request.getOperatorId()',
                expression: '${当前操作人ID}',
                tag: VariableTag.OPERATOR,
                order: 2,
            },
            {
                label: '是否管理员',
                value: 'request.getIsFlowManager()',
                expression: '${是否管理员}',
                tag: VariableTag.OPERATOR,
                order: 3,
            },
            {
                label: '流程创建人',
                value: 'request.getCreatorName()',
                expression: '${流程创建人}',
                tag: VariableTag.OPERATOR,
                order: 4,
            },

            // ========== 流程相关 ==========
            {
                label: '流程标题',
                value: 'request.getWorkflowTitle()',
                expression: '${流程标题}',
                tag: VariableTag.WORKFLOW,
                order: 10,
            },
            {
                label: '流程编码',
                value: 'request.getWorkflowCode()',
                expression: '${流程编码}',
                tag: VariableTag.WORKFLOW,
                order: 11,
            },
            {
                label: '当前节点',
                value: 'request.getNodeName()',
                expression: '${当前节点}',
                tag: VariableTag.WORKFLOW,
                order: 12,
            },
            {
                label: '节点类型',
                value: 'request.getNodeType()',
                expression: '${节点类型}',
                tag: VariableTag.WORKFLOW,
                order: 13,
            },

            // ========== 流程编号 ==========
            {
                label: '流程编号',
                value: 'request.getWorkCode()',
                expression: '${流程编号}',
                tag: VariableTag.WORK_CODE,
                order: 20,
            }
        ];
    }

    getMainFormMetaVariables(formMeta: FlowFromMeta): GroovyVariableMapping[] {
        if (!formMeta || !formMeta.fields) {
            return [];
        }

        return formMeta.fields.map((field, index) => ({
            label: `${field.name}`,
            value: `request.getFormData('${field.code}')`,
            expression: "${" + `${field.name}` + "}",
            tag: VariableTag.FORM_FIELD,
            order: 100 + index,
        }));
    }

}
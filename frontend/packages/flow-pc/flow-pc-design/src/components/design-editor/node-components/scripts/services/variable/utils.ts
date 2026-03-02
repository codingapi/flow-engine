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
                value: 'request.operatorName',
                expression: 'request.getOperatorName()',
                tag: VariableTag.OPERATOR,
                order: 1,
            },
            {
                label: '当前操作人ID',
                value: 'request.operatorId',
                expression: 'request.getOperatorId()',
                tag: VariableTag.OPERATOR,
                order: 2,
            },
            {
                label: '是否管理员',
                value: 'request.isFlowManager',
                expression: 'request.getIsFlowManager()',
                tag: VariableTag.OPERATOR,
                order: 3,
            },
            {
                label: '流程创建人',
                value: 'request.creatorName',
                expression: 'request.getCreatorName()',
                tag: VariableTag.OPERATOR,
                order: 4,
            },

            // ========== 流程相关 ==========
            {
                label: '流程标题',
                value: 'request.workflowTitle',
                expression: 'request.getWorkflowTitle()',
                tag: VariableTag.WORKFLOW,
                order: 10,
            },
            {
                label: '流程编码',
                value: 'request.workflowCode',
                expression: 'request.getWorkflowCode()',
                tag: VariableTag.WORKFLOW,
                order: 11,
            },
            {
                label: '当前节点',
                value: 'request.nodeName',
                expression: 'request.getNodeName()',
                tag: VariableTag.WORKFLOW,
                order: 12,
            },
            {
                label: '节点类型',
                value: 'request.nodeType',
                expression: 'request.getNodeType()',
                tag: VariableTag.WORKFLOW,
                order: 13,
            },

            // ========== 流程编号 ==========
            {
                label: '流程编号',
                value: 'request.workCode',
                expression: 'request.getWorkCode()',
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
            value: `request.formData(${field.code})`,
            expression: `request.getFormData('${field.name}')`,
            tag: VariableTag.FORM_FIELD,
            order: 100 + index,
        }));
    }

}
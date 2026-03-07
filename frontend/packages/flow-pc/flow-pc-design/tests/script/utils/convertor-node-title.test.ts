import {describe, expect, it} from '@rstest/core';
import {FlowForm} from "@flow-engine/flow-types";
import {NodeTitleScriptUtils} from "@/components/script/services/node-title";
import {GroovyVariableUtil} from "@/components/script/utils/varibale";
import {GroovyScriptConvertorUtil} from "@flow-engine/flow-core";

describe('NodeTitleScriptUtils', () => {

    const form: FlowForm = {
        name: '请假单',
        code: 'leave',
        fields: [
            {
                id: '1',
                name: '天数',
                code: 'days',
                type: 'NUMBER',
                required: true,
                defaultValue: ''
            },
            {
                id: '2',
                name: '理由',
                code: 'desc',
                type: 'STRING',
                required: true,
                defaultValue: ''
            },
        ],
        subForms: []
    }

    const variables = GroovyVariableUtil.getVariables(form);

    describe('updateExpression', () => {
        it('node title script reset expression', () => {
            const script = `
// @CUSTOM_SCRIPT 
def run(request){
    return "你有一条" + request.getOperatorName() + "的" + request.getWorkflowTitle() + "待办消息 【" + request.getNodeName() + "】"
}
`;
            const result = NodeTitleScriptUtils.updateExpression(script, '你有一条${当前操作人}的${流程标题}待办消息 【${当前节点}】', variables)
            const title = GroovyScriptConvertorUtil.getScriptTitle(result);
            expect(title).toEqual("你有一条${当前操作人}的${流程标题}待办消息 【${当前节点}】");
        });
    });


    describe('addVariable', () => {
        it('node title script reset expression', () => {
            const script = `
// @CUSTOM_SCRIPT 
def run(request){
    return "你有一条" + request.getOperatorName() + "的" + request.getWorkflowTitle() + "待办消息 【" + request.getNodeName() + "】"
}
`;
            const variable = variables[0];
            const result = NodeTitleScriptUtils.addVariable(script, variable, variables)
            const title = GroovyScriptConvertorUtil.getScriptTitle(result);
            expect(title).toEqual("你有一条${当前操作人}的${流程标题}待办消息 【${当前节点}】${当前操作人}");
        });
    });
});
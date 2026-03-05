import {describe, expect, it} from '@rstest/core';
import {FlowFromMeta} from "@flow-engine/flow-types";
import {
    NodeTitleScriptUtils
} from "@/components/script/services/utils/node-title";
import {
    NodeTitleVariableService
} from "@/components/script/services/variable/node-title";
import {GroovyScriptConvertorUtil} from "@/components/script/utils/convertor";

describe('NodeTitleGroovyConvertor', () => {

    const form :FlowFromMeta= {
        name:'请假单',
        code:'leave',
        fields:[
            {
                id:'1',
                name:'天数',
                code:'days',
                type:'number',
                required:true,
                defaultValue:''
            },
            {
                id:'2',
                name:'理由',
                code:'desc',
                type:'string',
                required:true,
                defaultValue:''
            },
        ],
        subForms:[]
    }

    const nodeTitleVariableAdapter = new NodeTitleVariableService(form);

    describe('updateExpression', () => {
        it('node title script reset expression', () => {
            const script = `
// @CUSTOM_SCRIPT 
def run(request){
    return "你有一条" + request.getOperatorName() + "的" + request.getWorkflowTitle() + "待办消息 【" + request.getNodeName() + "】"
}
`;
            const variables =  nodeTitleVariableAdapter.getVariables();
            const result = NodeTitleScriptUtils.updateExpression(script,'你有一条${当前操作人}的${流程标题}待办消息 【${当前节点}】',variables)
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
            const variables =  nodeTitleVariableAdapter.getVariables();
            const variable = variables[0];
            const result = NodeTitleScriptUtils.addVariable(script,variable,variables)
            const title = GroovyScriptConvertorUtil.getScriptTitle(result);
            expect(title).toEqual("你有一条${当前操作人}的${流程标题}待办消息 【${当前节点}】${当前操作人}");
        });
    });
});
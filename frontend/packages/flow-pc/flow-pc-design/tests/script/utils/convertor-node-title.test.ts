import {describe, expect, it} from '@rstest/core';
import {FlowFromMeta} from "@flow-engine/flow-types";
import {
    NodeTitleGroovyConvertor
} from "@/components/script/services/convertor/node-title";
import {
    NodeTitleVariableAdapter
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

    const nodeTitleVariableAdapter = new NodeTitleVariableAdapter(form);

    describe('toExpression', () => {
        it('node title script to expression', () => {
            const script = `
// @SCRIPT_TITLE 您有一条待办消息\${当前操作人ID}\${当前操作人ID}\${流程创建人}\${是否管理员}   
def run(request){
return "您有一条待办消息" + request.getOperatorId() + request.getOperatorId() + request.getCreatorName() + request.getIsFlowManager()
}
`
            const nodeTitleGroovyConvertor = new NodeTitleGroovyConvertor(script, nodeTitleVariableAdapter.getVariables());
            const result = nodeTitleGroovyConvertor.toExpression()
            expect(result).toEqual("您有一条待办消息${当前操作人ID}${当前操作人ID}${流程创建人}${是否管理员}");
        });
    });


    describe('resetExpression', () => {
        it('node title script reset expression', () => {
            const script = `
// @CUSTOM_SCRIPT 
def run(request){
    return "你有一条" + request.getOperatorName() + "的" + request.getWorkflowTitle() + "待办消息 【" + request.getNodeName() + "】"
}
`
            const nodeTitleGroovyConvertor = new NodeTitleGroovyConvertor(script, nodeTitleVariableAdapter.getVariables());
            const result = nodeTitleGroovyConvertor.resetExpression('你有一条${当前操作人}的${流程标题}待办消息 【${当前节点}】')
            const title = GroovyScriptConvertorUtil.getScriptTitle(result);
            expect(title).toEqual("你有一条${当前操作人}的${流程标题}待办消息 【${当前节点}】");
        });
    });
});
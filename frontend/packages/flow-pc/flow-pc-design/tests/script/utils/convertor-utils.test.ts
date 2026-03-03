import {describe, expect, it} from '@rstest/core';
import {GroovyScriptConvertorUtil} from "@/components/script/services/convertor/utils";

describe('GroovyScriptUtil', () => {

    describe('getReturnScript1', () => {
        it('get groovy script return data', () => {
            const script = `
// @CUSTOM_SCRIPT
def run(request){
    return "你有一条" + request.getOperatorName() + "的" + request.getWorkflowTitle() + "待办消息 【" + request.getNodeName() + "】"
}`
            const result = GroovyScriptConvertorUtil.getReturnScript(script)
            expect(result).toEqual(`"你有一条" + request.getOperatorName() + "的" + request.getWorkflowTitle() + "待办消息 【" + request.getNodeName() + "】"`);
        });
    });

    describe('getReturnScript2', () => {
        it('get groovy script return data', () => {
            const script = `
def run(request){
    return [request.getCreateOperator(),request.getCurrentOperator()]
}`
            const result = GroovyScriptConvertorUtil.getReturnScript(script)
            expect(result).toEqual(`[request.getCreateOperator(),request.getCurrentOperator()]`);
        });
    });


    describe('getScriptTitle', () => {
        it('get groovy script display title', () => {
            const script = `
// @SCRIPT_TITLE 这是一个实例的标题            
def run(request){
    return "你有一条" + request.getOperatorName() + "的" + request.getWorkflowTitle() + "待办消息 【" + request.getNodeName() + "】"
}`
            const result = GroovyScriptConvertorUtil.getScriptTitle(script)
            expect(result).toEqual(`这是一个实例的标题`);
        });
    });

    describe('updateScriptTitle', () => {
        it('update groovy script display title', () => {
            const script = `
// @SCRIPT_TITLE 这是一个实例的标题            
def run(request){
    return "你有一条" + request.getOperatorName() + "的" + request.getWorkflowTitle() + "待办消息 【" + request.getNodeName() + "】"
}`
            const result = GroovyScriptConvertorUtil.updateScriptTitle(script,'这就是一个实例的标题');
            console.log(result);
            const title = GroovyScriptConvertorUtil.getScriptTitle(result)
            expect(title).toEqual(`这就是一个实例的标题`);
        });
    });
});
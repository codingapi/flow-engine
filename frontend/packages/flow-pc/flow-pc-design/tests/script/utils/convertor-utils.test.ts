import {describe, expect, it} from '@rstest/core';
import {GroovyScriptConvertorUtil} from "@/components/design-editor/node-components/scripts/services/convertor/utils";

describe('GroovyScriptUtil', () => {

    describe('getReturnScript', () => {
        it('should convert simple text to groovy script', () => {
            const script = ` def run(request){
// @CUSTOM_SCRIPT
return "你有一条" + request.getOperatorName() + "的" + request.getWorkflowTitle() + "待办消息 【" + request.getNodeName() + "】"
}`
            const result = GroovyScriptConvertorUtil.getReturnScript(script)
            expect(result).toEqual(`"你有一条" + request.getOperatorName() + "的" + request.getWorkflowTitle() + "待办消息 【" + request.getNodeName() + "】"`);
        });
    });
});
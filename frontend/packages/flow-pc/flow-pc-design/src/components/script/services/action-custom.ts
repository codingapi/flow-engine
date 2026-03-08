import {GroovyScriptConvertorUtil} from "@flow-engine/flow-core";

export class ActionCustomScriptUtils {

    public static update(trigger: string, script: string) {
        let groovy;
        if (script) {
            const returnData = GroovyScriptConvertorUtil.getReturnScript(script).trim();
            groovy = script.replace(returnData, `'${trigger}'`);
            groovy = GroovyScriptConvertorUtil.updateScriptMeta(groovy, `{"trigger":"${trigger}"}`);
        } else {
            groovy = `// @CUSTOM_SCRIPT 自定义脚本，返回的数据为动作类型
            // @SCRIPT_META {"trigger":"${trigger}"}
            def run(request){
                return '${trigger}';
            }
            `
        }
        return GroovyScriptConvertorUtil.formatScript(groovy);
    }


    public static getTrigger(script:string){
        const meta = GroovyScriptConvertorUtil.getScriptMeta(script);
        const data = JSON.parse(meta);
        if(data){
            return data.trigger;
        }
        return undefined;
    }
}
import {GroovyVariableMapping} from "@/components/script/typings";
import {GroovyScriptConvertorUtil} from "@flow-engine/flow-core";
import {GroovyVariableUtil} from "@/components/script/utils/varibale";

/**
 *  标题脚本工具
 *  针对脚本的使用，只是用了@SCRIPT_TITLE 属性
 */
export class NodeTitleScriptUtils {

    public static addVariable(script:string,variable: GroovyVariableMapping,variables:GroovyVariableMapping[]) {
        const returnExpression = GroovyScriptConvertorUtil.getReturnScript(script);
        const expression = `${returnExpression}` + ` + ${variable.value}`;
        let groovy = script.replace(returnExpression, expression);
        const scriptTitle = GroovyVariableUtil.toExpression(expression, variables);
        groovy = GroovyScriptConvertorUtil.updateScriptTitle(groovy, scriptTitle);
        return GroovyScriptConvertorUtil.formatScript(groovy);
    }


    public static updateExpression(script:string,expression:string,variables:GroovyVariableMapping[]){
        const returnExpression = GroovyScriptConvertorUtil.getReturnScript(script);
        const returnScript = GroovyVariableUtil.toScript(expression, variables);
        let groovy = script.replace(returnExpression, `${returnScript}`);
        groovy= GroovyScriptConvertorUtil.updateScriptTitle(groovy, expression);
        return GroovyScriptConvertorUtil.formatScript(groovy);
    }

}
import {GroovyScriptConverter, GroovyVariableMapping} from "@/components/script/typings";
import {GroovyScriptConvertorUtil} from "@/components/script/utils/convertor";


export class NodeTitleGroovyConvertor implements GroovyScriptConverter {

    private readonly script: string;
    private readonly mappings: GroovyVariableMapping[];


    constructor(script: string, mappings: GroovyVariableMapping[]) {
        this.script = script;
        this.mappings = mappings;
    }

    getScript(): string {
        return this.script;
    }

    toExpression(): string {
        return GroovyScriptConvertorUtil.getScriptTitle(this.script);
    }

    getDefaultScript(): string {
        return `
        // @SCRIPT_TITLE 您有一条待办消息
        def run(request){\nreturn \"您有一条待办消息\"\n}
        `;
    }

    addVariable(variable: GroovyVariableMapping): string {
        const returnExpression = GroovyScriptConvertorUtil.getReturnScript(this.script);
        const newExpression = `${returnExpression}` + ` + ${variable.value}`;
        const script = this.script.replace(returnExpression, newExpression);
        const expression = GroovyScriptConvertorUtil.toExpression(newExpression, this.mappings);
        const groovy = GroovyScriptConvertorUtil.updateScriptTitle(script, expression);
        return GroovyScriptConvertorUtil.formatScript(groovy);
    }

    resetExpression(expression: string) {
        const returnExpression = GroovyScriptConvertorUtil.getReturnScript(this.script);
        const returnScript = GroovyScriptConvertorUtil.toScript(expression, this.mappings);
        const script = this.script.replace(returnExpression, `${returnScript}`);
        const groovy= GroovyScriptConvertorUtil.updateScriptTitle(script, expression);
        return GroovyScriptConvertorUtil.formatScript(groovy);
    }
}
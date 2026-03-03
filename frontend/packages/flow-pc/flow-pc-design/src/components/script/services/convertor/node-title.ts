import {GroovyVariableMapping} from "@/components/script/typings";
import {GroovyScriptConvertorUtil} from "@/components/script/utils/convertor";
import {DEFAULT_NODE_TITLE_SCRIPT} from "@/components/script/default-script";


export class NodeTitleGroovyConvertor  {

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
        return DEFAULT_NODE_TITLE_SCRIPT;
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
import {GroovyVariableMapping} from "@/components/design-editor/typings/script";
import {GroovyScriptConverter, GroovyScriptConvertorUtil} from "./utils";


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
        const returnExpression = GroovyScriptConvertorUtil.getReturnScript(this.script);
        return GroovyScriptConvertorUtil.toExpression(returnExpression, this.mappings);
    }

    getDefaultScript(): string {
        return "def run(request){\nreturn \"您有一条待办消息\"\n}";
    }

    addVariable(variable: GroovyVariableMapping): string {
        const returnExpression = GroovyScriptConvertorUtil.getReturnScript(this.script);
        const newExpression = `${returnExpression}` + ` + ${variable.value}`;
        return this.script.replace(returnExpression, newExpression);
    }

    resetExpression(expression:string) {
        const returnExpression = GroovyScriptConvertorUtil.getReturnScript(this.script);
        const script = GroovyScriptConvertorUtil.toScript(expression, this.mappings);
        return this.script.replace(returnExpression, `${script}`);
    }
}
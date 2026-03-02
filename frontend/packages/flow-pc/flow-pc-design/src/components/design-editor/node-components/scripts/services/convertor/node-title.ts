import {GroovyVariableMapping} from "@/components/design-editor/typings/script";
import {GroovyScriptConverter, GroovyScriptUtil} from "./utils";


export class NodeTitleGroovyConvertor implements GroovyScriptConverter {

    private readonly script: string;
    private readonly mappings: GroovyVariableMapping[];


    constructor(script: string, mappings: GroovyVariableMapping[]) {
        this.script = script;
        this.mappings = mappings;
    }

    toScript(): string {
        return this.script;
    }

    toExpression(): string {
        const returnExpression = GroovyScriptUtil.getReturnExpression(this.script);
        return GroovyScriptUtil.toExpression(returnExpression, this.mappings);
    }

    getDefaultScript(): string {
        return "def run(request){\nreturn \"您有一条待办消息\"\n}";
    }

    addVariable(variable: GroovyVariableMapping): string {
        const returnExpression = GroovyScriptUtil.getReturnExpression(this.script);
        const newExpression = `${returnExpression}` + ` + ${variable.value}`;
        return this.script.replace(returnExpression, newExpression);
    }

    resetExpression(value:string) {
        const returnExpression = GroovyScriptUtil.getReturnExpression(this.script);
        return this.script.replace(returnExpression, `"${value}"`);
    }
}
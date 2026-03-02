import {GroovyVariableMapping} from "@/components/design-editor/typings/script";
import {FlowFromMeta} from "@flow-engine/flow-types";
import {GroovyVariableUtil,GroovyVariableAdapter} from "./utils";

export class NodeTitleVariableAdapter implements GroovyVariableAdapter {

    private readonly form: FlowFromMeta;
    private readonly variableUtil: GroovyVariableUtil;

    constructor(form: FlowFromMeta) {
        this.form = form;
        this.variableUtil = new GroovyVariableUtil();
    }

    getVariables(): GroovyVariableMapping[] {
        const variables: GroovyVariableMapping[] = [];
        // 添加默认变量
        variables.push(...this.variableUtil.getDefaultVariables());
        // 添加表单字段变量
        variables.push(...this.variableUtil.getMainFormMetaVariables(this.form));
        return variables;
    }

}

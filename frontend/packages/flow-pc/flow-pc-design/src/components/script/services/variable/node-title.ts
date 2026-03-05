import {FlowForm} from "@flow-engine/flow-types";
import {GroovyVariableMapping} from "@/components/script/typings";
import {GroovyVariableUtil} from "@/components/script/utils/varibale";

export class NodeTitleVariableService {

    private readonly form: FlowForm;
    private readonly variableUtil: GroovyVariableUtil;

    constructor(form: FlowForm) {
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

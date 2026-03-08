import {GroovyScriptConvertorUtil} from "@flow-engine/flow-core";
import {FlowNode} from "@/components/design-panel/types";


export class RejectNodeManager {

    private readonly nodes:FlowNode[];

    constructor(nodes: FlowNode[]) {
        this.nodes = nodes;
    }

    public getSize(){
        return this.nodes.length;
    }
}

export class ActionRejectScriptUtils {

    public static update(type: string, script: string) {
        let groovy;
        if (script) {
            const returnData = GroovyScriptConvertorUtil.getReturnScript(script).trim();
            groovy = script.replace(returnData, `'${type}'`);
            groovy = GroovyScriptConvertorUtil.updateScriptMeta(groovy, `{"type":"${type}"}`);
        } else {
            groovy = `// @CUSTOM_SCRIPT 自定义脚本，返回的数据为动作类型
            // @SCRIPT_META {"type":"${type}"}
            def run(request){
                return '${type}';
            }
            `
        }
        return GroovyScriptConvertorUtil.formatScript(groovy);
    }


    public static getType(script:string){
        const meta = GroovyScriptConvertorUtil.getScriptMeta(script);
        const data = JSON.parse(meta);
        if(data){
            return data.type;
        }
        return undefined;
    }
}
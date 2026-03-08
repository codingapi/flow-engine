import {GroovyScriptConvertorUtil} from "@flow-engine/flow-core";
import {ActionSelectOption} from "@/components/script/typings";
import {NodeManger} from "@/components/design-panel/manager/node";

export class ActionRejectService {

    private readonly nodeManger: NodeManger;

    public constructor(nodeManger: NodeManger) {
        this.nodeManger = nodeManger;
    }

    public getOptions(nodeId: string) {
        const options: ActionSelectOption[] = [];
        const backNodes = this.nodeManger.getBackNodes(nodeId);
        for (const node of backNodes) {
            options.push({
                label: node.name,
                value: node.id,
            })
        }
        options.push({
            label: '终止流程',
            value: 'TERMINATE',
        })
        return options;
    }

    public getValue(script?: string) {
        const type = ActionRejectScriptUtils.getType(script);
        const node = this.nodeManger.getNodeByType(type);
        if (node) {
            return node.id;
        }
        return type;
    }

    public getScript(option: ActionSelectOption) {
        return ActionRejectScriptUtils.update(option);
    }

}

export class ActionRejectScriptUtils {

    public static update(option: ActionSelectOption) {
        const groovy = `// @CUSTOM_SCRIPT 跳转到 ${option.label}  
            // @SCRIPT_META {"type":"${option.value}"}
            def run(request){
                return '${option.value}';
            }
            `
        return GroovyScriptConvertorUtil.formatScript(groovy);
    }


    public static getType(script?: string) {
        if (script) {
            const meta = GroovyScriptConvertorUtil.getScriptMeta(script);
            if (meta) {
                const data = JSON.parse(meta);
                if (data) {
                    return data.type;
                }
            }
        }
        return undefined;
    }
}
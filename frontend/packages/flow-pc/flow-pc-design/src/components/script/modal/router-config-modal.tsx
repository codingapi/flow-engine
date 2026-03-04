import React from "react";
import {GroovyVariableMapping, ScriptType} from "@/components/script/typings";
import {GroovyScriptContent, GroovyScriptModal} from "@/components/script/components/groovy-script-modal";
import {GroovyScriptConvertorUtil} from "@/components/script/utils/convertor";
import {AdvancedScriptEditor} from "@/components/script/components/advanced-script-editor";
import {DEFAULT_ROUTER_SCRIPT} from "@/components/script/default-script";
import {RouterPluginView} from "@/components/script/plugins/view/router-view";

export interface RouterConfigModalProps {
    /** 是否展示 **/
    open: boolean;
    /** 当前脚本 */
    script: string;
    /** 表单字段（用于动态生成变量） */
    variables?: GroovyVariableMapping[];
    /** 取消回调 */
    onCancel: () => void;
    /** 确认回调 */
    onConfirm: (script: string) => void;
}



const RouterConfigContent: React.FC<GroovyScriptContent> = (props) => {
    const isAdvance = GroovyScriptConvertorUtil.isCustomScript(props.script);

    return (
        <>
            {isAdvance && (
                <AdvancedScriptEditor
                    {...props}
                    resetScript={()=>{
                        return DEFAULT_ROUTER_SCRIPT;
                    }}
                />
            )}
            {!isAdvance && (
                <RouterPluginView {...props} />
            )}
        </>
    );
}

export const RouterConfigModal:React.FC<RouterConfigModalProps> = (props) => {
    return (
        <GroovyScriptModal
            type={ScriptType.ROUTER}
            open={props.open}
            script={props.script}
            variables={props.variables || []}
            onConfirm={props.onConfirm}
            onCancel={props.onCancel}
            title="理由配置"
            content={RouterConfigContent}
        />
    );
}
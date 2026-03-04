import React from "react";
import {GroovyVariableMapping, ScriptType} from "@/components/script/typings";
import {GroovyScriptContent, GroovyScriptModal} from "@/components/script/components/groovy-script-modal";
import {GroovyScriptConvertorUtil} from "@/components/script/utils/convertor";
import {AdvancedScriptEditor} from "@/components/script/components/advanced-script-editor";
import {DEFAULT_TRIGGER_SCRIPT} from "@/components/script/default-script";
import {TriggerPluginView} from "@/components/script/plugins/view/trigger-view";

export interface TriggerConfigModalProps {
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



const TriggerConfigContent: React.FC<GroovyScriptContent> = (props) => {
    const isAdvance = GroovyScriptConvertorUtil.isCustomScript(props.script);

    return (
        <>
            {isAdvance && (
                <AdvancedScriptEditor
                    {...props}
                    resetScript={()=>{
                        return DEFAULT_TRIGGER_SCRIPT;
                    }}
                />
            )}
            {!isAdvance && (
                <TriggerPluginView {...props} />
            )}
        </>
    );
}

export const TriggerConfigModal:React.FC<TriggerConfigModalProps> = (props) => {
    return (
        <GroovyScriptModal
            type={ScriptType.TRIGGER}
            open={props.open}
            script={props.script}
            variables={props.variables || []}
            onConfirm={props.onConfirm}
            onCancel={props.onCancel}
            title="触发配置"
            content={TriggerConfigContent}
        />
    );
}
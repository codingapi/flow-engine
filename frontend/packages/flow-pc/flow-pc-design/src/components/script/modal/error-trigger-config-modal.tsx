import React from "react";
import {GroovyVariableMapping, ScriptType} from "@/components/script/typings";
import {GroovyScriptContent, GroovyScriptModal} from "@/components/script/components/groovy-script-modal";
import {GroovyScriptConvertorUtil} from "@flow-engine/flow-core";
import {AdvancedScriptEditor} from "@/components/script/components/advanced-script-editor";
import {SCRIPT_DEFAULT_ERROR_TRIGGER} from "@/components/script/default-script";
import {ErrorTriggerPluginView} from "@/components/script/plugins/view/error-trigger-view";

export interface ErrorTriggerConfigModalProps {
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



const ErrorTriggerConfigContent: React.FC<GroovyScriptContent> = (props) => {
    const isAdvance = GroovyScriptConvertorUtil.isCustomScript(props.script);

    return (
        <>
            {isAdvance && (
                <AdvancedScriptEditor
                    {...props}
                    resetScript={()=>{
                        return SCRIPT_DEFAULT_ERROR_TRIGGER;
                    }}
                />
            )}
            {!isAdvance && (
                <ErrorTriggerPluginView {...props} />
            )}
        </>
    );
}

export const ErrorTriggerConfigModal:React.FC<ErrorTriggerConfigModalProps> = (props) => {
    return (
        <GroovyScriptModal
            type={ScriptType.ERROR_TRIGGER}
            open={props.open}
            script={props.script}
            variables={props.variables || []}
            onConfirm={props.onConfirm}
            onCancel={props.onCancel}
            title="异常配置"
            content={ErrorTriggerConfigContent}
        />
    );
}
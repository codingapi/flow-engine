import React from "react";
import {GroovyVariableMapping, ScriptType} from "@/components/script/typings";
import {GroovyScriptContent, GroovyScriptModal} from "@/components/script/components/groovy-script-modal";
import {GroovyScriptConvertorUtil} from "@/components/script/utils/convertor";
import {AdvancedScriptEditor} from "@/components/script/components/advanced-script-editor";
import {DEFAULT_CONDITION_SCRIPT} from "@/components/script/default-script";
import {ConditionPluginView} from "@/components/script/plugins/view/condition-view";

export interface ConditionConfigModalProps {
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



const ConditionConfigContent: React.FC<GroovyScriptContent> = (props) => {
    const isAdvance = GroovyScriptConvertorUtil.isCustomScript(props.script);

    return (
        <>
            {isAdvance && (
                <AdvancedScriptEditor
                    {...props}
                    resetScript={()=>{
                        return DEFAULT_CONDITION_SCRIPT;
                    }}
                />
            )}
            {!isAdvance && (
                <ConditionPluginView {...props} />
            )}
        </>
    );
}

export const ConditionConfigModal:React.FC<ConditionConfigModalProps> = (props) => {
    return (
        <GroovyScriptModal
            type={ScriptType.CONDITION}
            open={props.open}
            script={props.script}
            variables={props.variables || []}
            onConfirm={props.onConfirm}
            onCancel={props.onCancel}
            title="条件配置"
            content={ConditionConfigContent}
        />
    );
}
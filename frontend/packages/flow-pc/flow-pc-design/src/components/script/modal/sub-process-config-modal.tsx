import React from "react";
import {GroovyVariableMapping, ScriptType} from "@/components/script/typings";
import {GroovyScriptContent, GroovyScriptModal} from "@/components/script/components/groovy-script-modal";
import {GroovyScriptConvertorUtil} from "@/components/script/utils/convertor";
import {AdvancedScriptEditor} from "@/components/script/components/advanced-script-editor";
import {DEFAULT_SUB_PROCESS_SCRIPT} from "@/components/script/default-script";
import {SubProcessPluginView} from "@/components/script/plugins/view/sub-process-view";

export interface SubProcessConfigModalProps {
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



const SubProcessConfigContent: React.FC<GroovyScriptContent> = (props) => {
    const isAdvance = GroovyScriptConvertorUtil.isCustomScript(props.script);

    return (
        <>
            {isAdvance && (
                <AdvancedScriptEditor
                    {...props}
                    resetScript={()=>{
                        return DEFAULT_SUB_PROCESS_SCRIPT;
                    }}
                />
            )}
            {!isAdvance && (
                <SubProcessPluginView {...props} />
            )}
        </>
    );
}

export const SubProcessConfigModal:React.FC<SubProcessConfigModalProps> = (props) => {
    return (
        <GroovyScriptModal
            type={ScriptType.SUB_PROCESS}
            open={props.open}
            script={props.script}
            variables={props.variables || []}
            onConfirm={props.onConfirm}
            onCancel={props.onCancel}
            title="异常配置"
            content={SubProcessConfigContent}
        />
    );
}
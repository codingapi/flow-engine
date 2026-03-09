import React from "react";
import {GroovyScriptContent, GroovyScriptModal} from "@/components/script/components/groovy-script-modal";
import {ScriptType} from "@/components/script/typings";
import {GroovyScriptConvertorUtil} from "@flow-engine/flow-core";
import {AdvancedScriptEditor} from "@/components/script/components/advanced-script-editor";
import {OperatorCreatePluginView} from "@/plugins/view/operator-create-view";
import {SCRIPT_DEFAULT_OPERATOR_CREATE} from "@/components/script/default-script";


interface OperatorCreateConfigModalProps{
    /** 是否展示 **/
    open: boolean;
    /** 当前脚本 */
    script: string;
    /** 取消回调 */
    onCancel: () => void;
    /** 确认回调 */
    onConfirm: (script: string) => void;
}


const OperatorCreateConfigContent :React.FC<GroovyScriptContent> = (props)=>{

    const isAdvance = GroovyScriptConvertorUtil.isCustomScript(props.script);

    return (
        <>
            {isAdvance && (
                <AdvancedScriptEditor
                    {...props}
                    resetScript={()=>{
                        return SCRIPT_DEFAULT_OPERATOR_CREATE;
                    }}
                />
            )}
            {!isAdvance && (
                <OperatorCreatePluginView script={props.script} onChange={props.onChange} />
            )}
        </>
    );
}


export const OperatorCreateConfigModal:React.FC<OperatorCreateConfigModalProps> = (props) => {

    return (
        <GroovyScriptModal
            title={"发起人员配置"}
            type={ScriptType.OPERATOR_CREATE}
            variables={[]}
            script={props.script}
            content={OperatorCreateConfigContent}
            open={props.open}
            onCancel={props.onCancel}
            onConfirm={props.onConfirm}
        />
    )
}
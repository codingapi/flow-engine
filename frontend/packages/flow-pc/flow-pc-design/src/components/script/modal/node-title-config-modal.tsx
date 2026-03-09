import React from 'react';
import {GroovyScriptContent, GroovyScriptModal} from "../components/groovy-script-modal";
import {AdvancedScriptEditor} from "@/components/script/components/advanced-script-editor";
import {GroovyVariableMapping, ScriptType} from "@/components/script/typings";
import {GroovyScriptConvertorUtil} from "@flow-engine/flow-core";
import {NodeTitlePluginView} from "@/plugins/view/node-title-view";
import {SCRIPT_DEFAULT_NODE_TITLE} from "@/components/script/default-script";

export interface NodeTitleConfigModalProps {
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

const NodeTitleConfigContent: React.FC<GroovyScriptContent> = (props) => {
    const isAdvance = GroovyScriptConvertorUtil.isCustomScript(props.script);

    return (
        <>
            {isAdvance && (
                <AdvancedScriptEditor
                    {...props}
                    resetScript={()=>{
                        return SCRIPT_DEFAULT_NODE_TITLE;
                    }}
                />
            )}
            {!isAdvance && (
                <NodeTitlePluginView {...props} />
            )}
        </>
    );
}

/**
 * 标题配置弹框
 * 支持普通模式和高级模式
 */
export const NodeTitleConfigModal: React.FC<NodeTitleConfigModalProps> = (props) => {

    return (
        <GroovyScriptModal
            type={ScriptType.TITLE}
            open={props.open}
            script={props.script}
            variables={props.variables || []}
            onConfirm={props.onConfirm}
            onCancel={props.onCancel}
            title="标题配置"
            content={NodeTitleConfigContent}
        />
    );
};



import React from 'react';
import {Button, Input, Space} from 'antd';
import {GroovyScriptContent, GroovyScriptModal} from "./components/groovy-script-modal";
import {GroovyScriptPreview} from "@/components/script/components/groovy-script-preview";
import {CodeOutlined, DeleteOutlined} from "@ant-design/icons";
import {NodeTitleGroovyConvertor} from "./services/convertor/node-title";
import {
    AdvancedScriptEditor
} from "@/components/script/components/advanced-script-editor";
import {VariablePicker} from "@/components/script/components/variable-picker";
import {GroovyVariableMapping, ScriptType} from "@/components/script/typings";
import {GroovyScriptConvertorUtil} from "@/components/script/utils/convertor";

const {TextArea} = Input;

export interface NodeTitleConfigModalProps {
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


const NodeTitleDefaultContent: React.FC<GroovyScriptContent> = (props) => {

    const nodeTitleGroovyConvertor = new NodeTitleGroovyConvertor(props.content, props.variables);

    const title = GroovyScriptConvertorUtil.getScriptTitle(props.content);

    return (
        <div>
            <div>
                预览
                <GroovyScriptPreview
                    script={props.content}
                    multiline={true}
                />
            </div>


            <div>
                内容
                <TextArea
                    value={title}
                    onChange={(e) => {
                        props.onChange(nodeTitleGroovyConvertor.resetExpression(e.target.value));
                    }}
                    placeholder="请输入标题配置脚本，支持使用变量"
                    autoSize={{minRows: 3, maxRows: 12}}
                />
            </div>

            <Space
                style={{
                    marginTop: 8
                }}
            >
                <VariablePicker
                    mappings={props.variables}
                    onSelect={(variable) => {
                        props.onChange(nodeTitleGroovyConvertor.addVariable(variable));
                    }}
                />
                <Button
                    icon={<CodeOutlined/>}
                    onClick={() => {
                        props.onChange(GroovyScriptConvertorUtil.toCustomScript(props.content));
                    }}
                >
                    高级配置
                </Button>
                <Button
                    icon={<DeleteOutlined/>}
                    danger={true}
                    onClick={() => {
                        props.onChange(nodeTitleGroovyConvertor.getDefaultScript());
                    }}
                >
                    重置配置
                </Button>
            </Space>
        </div>
    );
}


const NodeTitleConfigContent: React.FC<GroovyScriptContent> = (props) => {
    const isAdvance = GroovyScriptConvertorUtil.isCustomScript(props.content);

    console.log('NodeTitleConfigContent script', props.content);

    return (
        <>
            {isAdvance && (
                <AdvancedScriptEditor {...props} />
            )}
            {!isAdvance && (
                <NodeTitleDefaultContent {...props} />
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
            scriptType={ScriptType.TITLE}
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



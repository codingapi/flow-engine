import React from 'react';
import {Button, Input, Space} from 'antd';
import {GroovyVariableMapping, ScriptType} from "@/components/design-editor/typings/script";
import {GroovyScriptContent, GroovyScriptModal} from "./components/groovy-script-modal";
import {GroovyScriptPreview} from "@/components/design-editor/node-components/scripts/components/groovy-script-preview";
import {CodeOutlined, DeleteOutlined} from "@ant-design/icons";
import {NodeTitleGroovyConvertor} from "./services/convertor/node-title";
import ScriptEditor from "@/components/design-editor/node-components/scripts/components/script-editor";
import {GroovyScriptUtil} from "@/components/design-editor/node-components/scripts/services/convertor/utils";
import {VariablePicker} from "@/components/design-editor/node-components/scripts/components/variable-picker";

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


const NodeTitleDefaultContent: React.FC<GroovyScriptContent> = (props)=>{

    const nodeTitleGroovyConvertor = new NodeTitleGroovyConvertor(props.content, props.variables);

    return (
        <div>
            <div>
                预览
                <GroovyScriptPreview
                    value={nodeTitleGroovyConvertor.toExpression()}
                    multiline={true}
                />
            </div>


            <div>
                内容
                <TextArea
                    value={nodeTitleGroovyConvertor.toExpression()}
                    onChange={(e) => {
                        props.onChange(nodeTitleGroovyConvertor.resetExpression(e.target.value));
                    }}
                    placeholder="请输入标题配置脚本，支持使用变量"
                    autoSize={{minRows: 3, maxRows: 12}}
                />
            </div>

            <Space
                style={{
                    marginTop:8
                }}
            >
                <VariablePicker
                    mappings={props.variables}
                    onSelect={(variable)=>{
                        props.onChange(nodeTitleGroovyConvertor.addVariable(variable));
                    }}
                />
                <Button
                    icon={<CodeOutlined />}
                    onClick={()=>{
                        props.onChange(GroovyScriptUtil.toCustomScript(props.content));
                    }}
                >
                    高级配置
                </Button>
                <Button
                    icon={<DeleteOutlined />}
                    danger={true}
                    onClick={()=>{
                        props.onChange(nodeTitleGroovyConvertor.getDefaultScript());
                    }}
                >
                    重置配置
                </Button>
            </Space>
        </div>
    );
}

const NodeTitleAdvancedContent: React.FC<GroovyScriptContent> = (props) => {

    const nodeTitleGroovyConvertor = new NodeTitleGroovyConvertor(props.content, props.variables);

    return (
        <div>
            自定义脚本
            <ScriptEditor
                scriptType={ScriptType.TITLE}
                script={props.content}
                variables={props.variables}
                onChange={props.onChange}
            />
            <Space
                style={{
                    marginTop:8
                }}
            >
                <Button
                    icon={<DeleteOutlined />}
                    danger={true}
                    onClick={()=>{
                        props.onChange(nodeTitleGroovyConvertor.getDefaultScript());
                    }}
                >
                    重置配置
                </Button>
            </Space>
        </div>
    )
}


const NodeTitleConfigContent: React.FC<GroovyScriptContent> = (props) => {

    console.log('NodeTitleConfigContent script', props.content);

    const isAdvance =  GroovyScriptUtil.isCustomScript(props.content);

    return (
        <>
            {isAdvance && (
                <NodeTitleAdvancedContent {...props} />
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



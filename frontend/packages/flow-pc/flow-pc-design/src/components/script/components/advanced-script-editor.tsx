import {Button, Space} from 'antd';
import React from "react";
import {GroovyScriptContent} from "@/components/script/components/groovy-script-modal";
import {MenuUnfoldOutlined, ReloadOutlined} from "@ant-design/icons";
import {GroovyVariableMapping, ScriptType} from "@/components/script/typings";
import {GroovyCodeEditor} from './groovy-code-editor';
import {GroovyScriptConvertorUtil} from "@/components/script/utils/convertor";

interface ScriptEditorProps {
    /** 脚本类型 */
    type: ScriptType;
    /** 当前脚本内容 */
    script: string;
    /** 变量映射列表 */
    variables: GroovyVariableMapping[];
    /** 脚本变更回调 */
    onChange: (script: string) => void;
    /** 是否只读 */
    readonly?: boolean;
}

const hintStyle: React.CSSProperties = {
    fontSize: 12,
    color: 'rgba(0, 0, 0, 0.25)',
};

/**
 * 高级脚本编辑器组件
 * 支持自由编辑 Groovy 脚本
 */
export const ScriptEditor: React.FC<ScriptEditorProps> = (props: ScriptEditorProps) => {
    const {script, onChange, readonly = false, variables} = props;

    const handleChange = (value: string) => {
        if (!readonly) {
            onChange(value);
        }
    };

    return (
        <GroovyCodeEditor
            value={script}
            readonly={readonly}
            onChange={handleChange}
            placeholder={"请输入 Groovy 脚本..."}
            theme={'dark'}
            options={{
                fontSize: 14,
                minHeight: 300,
                maxHeight: 300,
            }}
        />
    );
}


interface AdvancedScriptEditorProps extends GroovyScriptContent {
    //重置脚本
    resetScript(): string;
}

export const AdvancedScriptEditor: React.FC<AdvancedScriptEditorProps> = (props) => {

    return (
        <div>
            <ScriptEditor
                type={props.type}
                script={props.script}
                variables={props.variables || []}
                onChange={props.onChange}
            />
            <Space
                style={{
                    marginTop: 8
                }}
            >
                <Button
                    icon={<MenuUnfoldOutlined/>}
                    onClick={() => {
                        props.onChange(GroovyScriptConvertorUtil.formatScript(props.script));
                    }}
                >
                    格式化代码
                </Button>

                <Button
                    icon={<ReloadOutlined/>}
                    danger={true}
                    onClick={() => {
                        props.onChange(props.resetScript());
                    }}
                >
                    重置脚本
                </Button>
            </Space>
            <div style={{marginTop: 8}}>
                <div style={hintStyle}>
                    {'// @CUSTOM_SCRIPT 是自定义脚本的标识'}
                </div>
                <div style={hintStyle}>
                    {'// @SCRIPT_TITLE 是脚本的展示内容'}
                </div>
            </div>
        </div>
    )
}
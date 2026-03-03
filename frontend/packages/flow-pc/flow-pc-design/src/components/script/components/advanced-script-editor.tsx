import {Button, Input, Space} from 'antd';
import React from "react";
import {GroovyScriptContent} from "@/components/script/components/groovy-script-modal";
import {DeleteOutlined} from "@ant-design/icons";
import {GroovyVariableMapping, ScriptType} from "@/components/script/typings";
import {
    GroovyScriptConverterContext
} from "@/components/script/context/groovy-script-convertor-context";

const {TextArea} = Input;

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

/**
 * 高级脚本编辑器组件
 * 支持自由编辑 Groovy 脚本
 */
export const ScriptEditor:React.FC<ScriptEditorProps> = (props: ScriptEditorProps)=> {
    const {script, onChange, readonly = false, variables} = props;

    const handleChange = (value: string) => {
        if (!readonly) {
            onChange(value);
        }
    };

    return (
        <TextArea
            value={script}
            onChange={(e) => handleChange(e.target.value)}
            readOnly={readonly}
            style={{ fontFamily: 'Courier New, monospace' }}
            placeholder="请输入 Groovy 脚本..."
            autoSize={{ minRows: 6, maxRows: 10 }}
        />
    );
}


export const AdvancedScriptEditor: React.FC<GroovyScriptContent> = (props) => {

    const groovyScriptConvertor = GroovyScriptConverterContext.getInstance().createConverter(props.type, props.content, props.variables);

    if(groovyScriptConvertor) {

        return (
            <div>
                自定义脚本
                <ScriptEditor
                    type={props.type}
                    script={props.content}
                    variables={props.variables}
                    onChange={props.onChange}
                />
                <Space
                    style={{
                        marginTop: 8
                    }}
                >
                    <Button
                        icon={<DeleteOutlined/>}
                        danger={true}
                        onClick={() => {
                            props.onChange(groovyScriptConvertor.getDefaultScript());
                        }}
                    >
                        重置配置
                    </Button>
                </Space>
            </div>
        )
    }
}
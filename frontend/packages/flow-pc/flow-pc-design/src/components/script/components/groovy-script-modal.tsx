import React from 'react';
import {Modal} from 'antd';
import {GroovyVariableMapping, ScriptType} from "@/components/script/typings";


export interface GroovyScriptModalProps {
    open: boolean;
    /** 脚本类型 */
    type: ScriptType;
    /** 当前脚本 */
    script: string;
    /** 变量映射列表 */
    variables: GroovyVariableMapping[];
    /** 确认回调 */
    onConfirm: (script: string) => void;
    /** 取消回调 */
    onCancel: () => void;
    // 宽度
    width?:number|string;
    /** 弹框标题 */
    title?: string;
    /** 自定义内容组件（可选） */
    content:React.ComponentType<GroovyScriptContent>;
}


export interface GroovyScriptContent {
    /** 脚本类型 */
    type: ScriptType;
    /** 当前脚本 */
    script: string;
    /** 变量映射列表 */
    variables: GroovyVariableMapping[];
    /** 确认回调 */
    onChange: (script: string) => void;
}

interface GroovyScriptContentComponentAction{
    handleConfirm: () => void;
}

interface GroovyScriptContentComponentProps extends GroovyScriptModalProps{
    actionRef:React.Ref<GroovyScriptContentComponentAction>;
}

const GroovyScriptContentComponent: React.FC<GroovyScriptContentComponentProps> = (props) => {
    const [content, setContent] = React.useState(props.script);
    const GroovyContent = props.content;

    React.useImperativeHandle(props.actionRef,()=>{
        return {
            handleConfirm:() => {
                props.onConfirm(content);
                props.onCancel();
            }
        }
    },[content]);

    return (
        <GroovyContent
            type={props.type}
            script={content}
            variables={props.variables}
            onChange={setContent}
        />
    )
}

/**
 * 通用脚本配置弹框
 * 支持普通模式和高级模式切换
 */
export const GroovyScriptModal: React.FC<GroovyScriptModalProps> = (props) => {

    const actionRef = React.useRef<GroovyScriptContentComponentAction>(null);

    const width = props.width || 800;
    const handleOk = () => {
        if (actionRef.current) {
            actionRef.current.handleConfirm();
        }
    }

    return (
        <Modal
            title={props.title}
            open={props.open}
            onCancel={props.onCancel}
            onOk={handleOk}
            width={width}
            okText="确认"
            cancelText="取消"
            destroyOnHidden={true}
        >
            <GroovyScriptContentComponent
                {...props}
                actionRef={actionRef}
            />
        </Modal>
    );
};


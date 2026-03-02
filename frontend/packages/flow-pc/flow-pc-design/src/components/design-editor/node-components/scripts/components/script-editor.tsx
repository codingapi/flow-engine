import {ScriptType,GroovyVariableMapping} from '@/components/design-editor/typings/script';
import {Input} from 'antd';

const {TextArea} = Input;

interface ScriptEditorProps {
    /** 脚本类型 */
    scriptType: ScriptType;
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
export function ScriptEditor(props: ScriptEditorProps) {
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

export default ScriptEditor;

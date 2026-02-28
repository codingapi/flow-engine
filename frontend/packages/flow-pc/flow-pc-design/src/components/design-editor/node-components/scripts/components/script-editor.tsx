import { useState } from 'react';
import { GroovyVariableMapping } from '@flow-engine/flow-types';
import { ScriptType } from '@/components/design-editor/typings/groovy-script';

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
  const { script, onChange, readonly = false, variables } = props;

  const handleChange = (value: string) => {
    if (!readonly) {
      onChange(value);
    }
  };

  return (
    <div className="script-editor">
      <textarea
        className="script-editor-textarea"
        value={script}
        onChange={(e) => handleChange(e.target.value)}
        readOnly={readonly}
        placeholder="请输入 Groovy 脚本..."
        rows={10}
      />
    </div>
  );
}

export default ScriptEditor;

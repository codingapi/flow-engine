import React, { useState, useEffect, useRef } from 'react';
import { Modal, Input, Alert, Button, Space, message } from 'antd';
import { EditOutlined, CodeOutlined } from '@ant-design/icons';
import { GroovyVariableMapping } from '@flow-engine/flow-types';
import { ScriptType } from '@/components/design-editor/typings/groovy-script';
import { groovySyntaxConverter } from '@/components/design-editor/script/service/groovy-syntax-converter';
import { ScriptEditor } from './script-editor';

const { TextArea } = Input;

export interface ScriptConfigModalProps {
  /** 脚本类型 */
  scriptType: ScriptType;
  /** 当前脚本 */
  script: string;
  /** 变量映射列表 */
  variables: GroovyVariableMapping[];
  /** 确认回调 */
  onConfirm: (script: string) => void;
  /** 取消回调 */
  onCancel: () => void;
  /** 弹框标题 */
  title?: string;
}

/**
 * 通用脚本配置弹框
 * 支持普通模式和高级模式切换
 */
export const ScriptConfigModal: React.FC<ScriptConfigModalProps> = ({
  scriptType,
  script,
  variables,
  onConfirm,
  onCancel,
  title = '脚本配置',
}) => {
  const [mode, setMode] = useState<'normal' | 'advanced'>('normal');
  const [content, setContent] = useState('');
  const [cursorPosition, setCursorPosition] = useState(0);
  const [variablePickerOpen, setVariablePickerOpen] = useState(false);
  const textareaRef = useRef<any>(null);
  const userModifiedModeRef = useRef(false);

  useEffect(() => {
    if (userModifiedModeRef.current) {
      return;
    }

    const isAdvanced = groovySyntaxConverter.isAdvancedMode(script);
    const parsedMode = isAdvanced ? 'advanced' : 'normal';

    if (parsedMode === 'normal') {
      const labelExpr = groovySyntaxConverter.toExpression(scriptType, script, variables);
      setContent(labelExpr || '');
    } else {
      setContent(script);
    }

    setMode(parsedMode);
  }, [script]);

  // 切换到高级模式
  const handleSwitchToAdvanced = () => {
    const groovyScript = groovySyntaxConverter.toScript(scriptType, content, variables);
    setContent(groovyScript);
    setMode('advanced');
    userModifiedModeRef.current = true;
  };

  // 切换到普通模式
  const handleSwitchToNormal = () => {
    const labelExpr = groovySyntaxConverter.toExpression(scriptType, content, variables);
    if (labelExpr === null) {
      message.error('当前脚本无法转换为可视化表达式，请检查语法');
      return;
    }
    setContent(labelExpr);
    setMode('normal');
    userModifiedModeRef.current = true;
  };

  // 确认
  const handleConfirm = () => {
    if (mode === 'normal') {
      const groovyScript = groovySyntaxConverter.toScript(scriptType, content, variables);
      onConfirm(groovyScript);
    } else {
      onConfirm(content);
    }
  };

  // 插入变量
  const handleInsertVariable = (mapping: GroovyVariableMapping) => {
    const variableText = `\${${mapping.label}}`;
    const start = cursorPosition;
    const newContent = content.substring(0, start) + variableText + content.substring(start);
    setContent(newContent);
    setCursorPosition(start + variableText.length);
    setVariablePickerOpen(false);

    setTimeout(() => {
      if (textareaRef.current) {
        textareaRef.current.focus();
        textareaRef.current.setSelectionRange(start + variableText.length, start + variableText.length);
      }
    }, 0);
  };

  // 预览
  const handlePreview = () => {
    if (mode === 'normal') {
      const groovyScript = groovySyntaxConverter.toScript(scriptType, content, variables);
      const labelExpr = groovySyntaxConverter.toExpression(scriptType, groovyScript, variables);
      message.info(labelExpr || '预览: ' + groovyScript);
    } else {
      message.info('预览: ' + content);
    }
  };

  // 变量选择器
  const renderVariablePicker = () => {
    if (!variablePickerOpen) {
      return null;
    }

    // 按 tag 分组
    const groups = new Map<string, GroovyVariableMapping[]>();
    for (const v of variables) {
      const group = groups.get(v.tag) || [];
      group.push(v);
      groups.set(v.tag, group);
    }

    return (
      <div style={{
        position: 'absolute',
        top: '100%',
        left: 0,
        right: 0,
        maxHeight: '200px',
        overflowY: 'auto',
        background: '#fff',
        border: '1px solid #d9d9d9',
        borderRadius: '6px',
        boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
        zIndex: 10,
        padding: '8px',
      }}>
        {Array.from(groups.entries()).map(([tag, vars]) => (
          <div key={tag}>
            <div style={{ fontWeight: 500, marginBottom: '4px', color: '#666' }}>{tag}</div>
            {vars.map(v => (
              <div
                key={v.label}
                onClick={() => {
                  handleInsertVariable(v);
                  setVariablePickerOpen(false);
                }}
                style={{
                  padding: '4px 8px',
                  cursor: 'pointer',
                  borderRadius: '4px',
                }}
                onMouseEnter={e => e.currentTarget.style.background = '#f5f5f5'}
                onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
              >
                <div>{v.label}</div>
                <div style={{ fontSize: '12px', color: '#999' }}>{v.value}</div>
              </div>
            ))}
          </div>
        ))}
      </div>
    );
  };

  return (
    <Modal
      title={title}
      open={true}
      onCancel={onCancel}
      onOk={handleConfirm}
      width={700}
      okText="确认"
      cancelText="取消"
    >
      <div className="script-config-modal">
        {/* 模式切换 */}
        <div style={{ marginBottom: '16px' }}>
          <Space>
            <Button
              type={mode === 'normal' ? 'primary' : 'default'}
              icon={<EditOutlined />}
              onClick={handleSwitchToNormal}
              disabled={mode === 'normal'}
            >
              普通模式
            </Button>
            <Button
              type={mode === 'advanced' ? 'primary' : 'default'}
              icon={<CodeOutlined />}
              onClick={handleSwitchToAdvanced}
              disabled={mode === 'advanced'}
            >
              高级模式
            </Button>
          </Space>
        </div>

        {/* 编辑器区域 */}
        {mode === 'normal' ? (
          <div style={{ position: 'relative' }}>
            <TextArea
              ref={textareaRef}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              onSelect={(e: any) => setCursorPosition(e.target.selectionStart)}
              placeholder="请输入表达式，可以使用 ${变量名} 插入变量"
              rows={4}
            />
            <div style={{ marginTop: '8px' }}>
              <Button
                type="link"
                onClick={() => setVariablePickerOpen(!variablePickerOpen)}
                style={{ padding: 0 }}
              >
                插入变量
              </Button>
              {renderVariablePicker()}
              <Button
                type="link"
                onClick={handlePreview}
                style={{ marginLeft: '8px' }}
              >
                预览
              </Button>
            </div>
          </div>
        ) : (
          <div>
            <Alert
              message="高级模式说明"
              description="在高级模式下，您可以自由编写 Groovy 脚本。保存后再打开将无法转换回普通模式。"
              type="warning"
              showIcon
              style={{ marginBottom: '16px' }}
            />
            <ScriptEditor
              scriptType={scriptType}
              script={content}
              variables={variables}
              onChange={setContent}
            />
          </div>
        )}
      </div>
    </Modal>
  );
};

export default ScriptConfigModal;

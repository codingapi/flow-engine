import React, { useState, useMemo, useRef } from 'react';
import { Button, Space } from 'antd';
import { EditOutlined } from '@ant-design/icons';
import { Field, FieldRenderProps } from '@flowgram.ai/fixed-layout-editor';
import { GroovyVariableService } from '@/services/groovy-variable-service';
import { TitleSyntaxConverter } from '@/utils/title-syntax-converter';
import { TitleConfigModal } from './TitleConfigModal';

/**
 * 节点标题策略配置
 */
export const NodeTitleStrategy: React.FC = () => {
  const [showConfigModal, setShowConfigModal] = useState(false);
  const [currentScript, setCurrentScript] = useState('');
  const [displayScript, setDisplayScript] = useState('');
  // 使用 ref 保存 onChange 回调，以便在弹框确认后更新编辑器状态
  const onChangeRef = useRef<((value: string) => void) | null>(null);

  // 获取表单字段（从context获取）
  const formFields = useMemo(() => {
    // TODO: 从 design context 获取当前流程的表单字段
    return [];
  }, []);

  // 获取变量映射
  const mappings = GroovyVariableService.getAllMappings(formFields);

  // 渲染预览内容
  const renderPreview = (script: string) => {
    if (!script) {
      return '（未配置）';
    }

    const mode = TitleSyntaxConverter.parseMode(script);
    if (mode === 'normal') {
      const labelExpr = TitleSyntaxConverter.toLabelExpression(script, mappings);
      return labelExpr || script;
    }

    // 尝试解析高级脚本
    const labelExpr = TitleSyntaxConverter.toLabelExpression(script, mappings);
    if (labelExpr !== null) {
      return labelExpr;
    }

    return '（自定义配置）';
  };

  const handleOpenConfig = () => {
    // 使用当前显示的脚本值
    setCurrentScript(displayScript);
    setShowConfigModal(true);
  };

  const handleConfirm = (script: string) => {
    // 更新显示的脚本值
    setDisplayScript(script);
    // 关键修复：调用 onChange 回调更新编辑器状态
    if (onChangeRef.current) {
      onChangeRef.current(script);
    }
    setShowConfigModal(false);
  };

  return (
    <>
      <div style={{ width: '100%' }}>
        <Field
          name="NodeTitleStrategy.script"
          render={({ field: { value, onChange } }: FieldRenderProps<any>) => {
            // 保存 onChange 回调到 ref
            onChangeRef.current = onChange;

            // 更新显示的脚本值（当编辑器状态改变时）
            if (value !== displayScript) {
              setDisplayScript(value || '');
            }

            return (
              <Space.Compact style={{ width: '100%' }}>
                <div
                  style={{
                    flex: 1,
                    padding: '4px 11px',
                    backgroundColor: value ? '#fff' : '#fafafa',
                    border: '1px solid #d9d9d9',
                    borderRadius: '6px 0 0 6px',
                    color: value ? 'rgba(0,0,0,0.88)' : 'rgba(0,0,0,0.25)',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                  }}
                >
                  {renderPreview(value)}
                </div>
                <Button
                  icon={<EditOutlined />}
                  onClick={handleOpenConfig}
                  style={{ borderRadius: '0 6px 6px 0' }}
                >
                  编辑
                </Button>
              </Space.Compact>
            );
          }}
        />
      </div>

      {showConfigModal && (
        <TitleConfigModal
          script={currentScript}
          formFields={formFields}
          onConfirm={handleConfirm}
          onCancel={() => setShowConfigModal(false)}
        />
      )}
    </>
  );
};

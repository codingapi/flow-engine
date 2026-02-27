import React, { useMemo, useRef, useState, useCallback } from 'react';
import { Form, Button, Space } from 'antd';
import { EditOutlined } from '@ant-design/icons';
import { Field, FieldRenderProps } from '@flowgram.ai/fixed-layout-editor';
import { useDesignContext } from '@/components/design-panel/hooks/use-design-context';
import { WorkflowFormManager } from '@/components/design-panel/manager/form';
import { GroovyVariableService } from '@/services/groovy-variable-service';
import { TitleSyntaxConverter } from '@/utils/title-syntax-converter';
import { TitleConfigModal } from './TitleConfigModal';

/**
 * 节点标题策略配置
 */
export const NodeTitleStrategy: React.FC = () => {
  const [showConfigModal, setShowConfigModal] = useState(false);
  const [modalScript, setModalScript] = useState('');
  // 使用 ref 保存 onChange 回调
  const onChangeRef = useRef<((value: string) => void) | null>(null);

  // 从 design context 获取表单字段
  const { state } = useDesignContext();

  // 获取表单字段（从 workflow form 中提取）
  const formFields = useMemo(() => {
    const fields: Array<{ name: string; code: string }> = [];
    if (!state?.workflow?.form) {
      return fields;
    }
    const formManager = new WorkflowFormManager(state.workflow.form);
    // 获取主表单字段
    const mainFields = formManager.getFormFields(state.workflow.form.code);
    for (const field of mainFields) {
      fields.push({ name: field.name, code: field.code });
    }
    // 获取子表单字段
    const subForms = state.workflow.form.subForms || [];
    for (const subForm of subForms) {
      const subFields = formManager.getFormFields(subForm.code);
      for (const field of subFields) {
        fields.push({ name: `${subForm.name}.${field.name}`, code: field.code });
      }
    }
    return fields;
  }, [state?.workflow?.form]);

  // 获取变量映射
  const mappings = GroovyVariableService.getAllMappings(formFields);

  // 渲染预览内容
  const renderPreview = useCallback((script: string) => {
    if (!script) {
      return '（未配置）';
    }

    const mode = TitleSyntaxConverter.parseMode(script);
    if (mode === 'normal') {
      // Normal 模式：尝试解析为标签表达式
      const labelExpr = TitleSyntaxConverter.toLabelExpression(script, mappings);
      return labelExpr || '（未配置）';
    }

    // Advanced 模式：显示"用户自定义配置"，不显示代码
    return '（自定义配置）';
  }, [mappings]);

  const handleOpenConfig = (currentValue: string) => {
    setModalScript(currentValue || '');
    setShowConfigModal(true);
  };

  const handleConfirm = useCallback((script: string) => {
    // 调用 onChange 回调更新编辑器状态
    if (onChangeRef.current) {
      onChangeRef.current(script);
    }
    setShowConfigModal(false);
  }, []);

  return (
    <>
      <Form style={{ width: '100%' }} layout="vertical">
        <Form.Item label="节点标题">
          <Field
            name="NodeTitleStrategy.script"
            render={(props: FieldRenderProps<any>) => {
              const { value, onChange } = props.field;
              // 保存 onChange 回调到 ref
              onChangeRef.current = onChange;

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
                    {renderPreview(value || '')}
                  </div>
                  <Button
                    icon={<EditOutlined />}
                    onClick={() => handleOpenConfig(value || '')}
                    style={{ borderRadius: '0 6px 6px 0' }}
                  >
                    编辑
                  </Button>
                </Space.Compact>
              );
            }}
          />
        </Form.Item>
      </Form>

      {showConfigModal && (
        <TitleConfigModal
          script={modalScript}
          formFields={formFields}
          onConfirm={handleConfirm}
          onCancel={() => setShowConfigModal(false)}
        />
      )}
    </>
  );
};

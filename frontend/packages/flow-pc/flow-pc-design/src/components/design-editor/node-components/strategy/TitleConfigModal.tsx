import React, { useState, useEffect, useRef } from 'react';
import { Modal, Input, Alert, Button, Space, message } from 'antd';
import { EditOutlined, CodeOutlined, RollbackOutlined } from '@ant-design/icons';
import { GroovyVariableMapping } from '@flow-engine/flow-types';
import { GroovyVariableService } from '@/services/groovy-variable-service';
import { TitleSyntaxConverter } from '@/utils/title-syntax-converter';
import { VariablePicker } from './VariablePicker';

const { TextArea } = Input;

export interface TitleConfigModalProps {
  /** 当前脚本 */
  script: string;
  /** 表单字段（用于动态生成变量） */
  formFields?: Array<{ name: string; code: string }>;
  /** 确认回调 */
  onConfirm: (script: string) => void;
  /** 取消回调 */
  onCancel: () => void;
}

/**
 * 标题配置弹框
 * 支持普通模式和高级模式
 */
export const TitleConfigModal: React.FC<TitleConfigModalProps> = ({
  script,
  formFields,
  onConfirm,
  onCancel,
}) => {
  const [mode, setMode] = useState<'normal' | 'advanced'>('normal');
  const [content, setContent] = useState('');
  const [showVariablePicker, setShowVariablePicker] = useState(false);

  // 标记用户是否手动修改了模式（防止 useEffect 覆盖用户操作）
  const userModifiedModeRef = useRef(false);

  // 获取变量映射
  const mappings = GroovyVariableService.getAllMappings(formFields);

  useEffect(() => {
    // 如果用户手动修改了模式，不再覆盖
    if (userModifiedModeRef.current) {
      return;
    }

    // 只在 script 改变时重新解析（初始化）
    const parsedMode = TitleSyntaxConverter.parseMode(script);

    if (parsedMode === 'normal') {
      // 尝试解析为标签表达式
      const labelExpr = TitleSyntaxConverter.toLabelExpression(script, mappings);
      // 如果是默认 legacy 脚本，显示空内容（让用户重新输入）
      // 如果能解析成标签表达式，显示解析结果
      // 否则显示空内容
      setContent(labelExpr || '');
    } else {
      // 高级模式，直接使用原脚本
      setContent(script);
    }

    setMode(parsedMode);
  }, [script]);

  // 插入变量
  const handleInsertVariable = (mapping: GroovyVariableMapping) => {
    setContent(prev => prev + `\${${mapping.label}}`);
  };

  // 切换到高级模式
  const handleSwitchToAdvanced = () => {
    userModifiedModeRef.current = true;
    if (mode === 'normal') {
      // 转换为Groovy脚本
      const groovyScript = TitleSyntaxConverter.toGroovySyntax(content, mappings);
      setContent(groovyScript);
    }
    setMode('advanced');
  };

  // 重置为普通模式
  const handleResetToNormal = () => {
    userModifiedModeRef.current = true;
    setContent('');
    setMode('normal');
  };

  // 确认
  const handleConfirm = () => {
    let finalScript = content;

    if (mode === 'normal') {
      if (!content.trim()) {
        message.error('请输入标题内容');
        return;
      }
      // 转换为Groovy脚本
      finalScript = TitleSyntaxConverter.toGroovySyntax(content, mappings);
    } else {
      // 验证Groovy语法
      const validation = TitleSyntaxConverter.validateGroovySyntax(content);
      if (!validation.valid) {
        message.error(`语法错误: ${validation.error}`);
        return;
      }
    }

    onConfirm(finalScript);
  };

  // 预览内容
  const renderPreview = () => {
    if (mode === 'normal') {
      return <div style={previewStyle}>{content || '（空）'}</div>;
    } else {
      // 尝试解析高级脚本
      const labelExpr = TitleSyntaxConverter.toLabelExpression(content, mappings);
      if (labelExpr !== null) {
        return <div style={previewStyle}>{labelExpr}</div>;
      }
      return (
        <Alert
          message="用户自定义配置，无法预览"
          type="warning"
          showIcon
          style={{ margin: 0 }}
        />
      );
    }
  };

  return (
    <>
      <Modal
        title="标题配置"
        open={true}
        onCancel={onCancel}
        width={600}
        footer={
          <Space>
            <Button onClick={onCancel}>取消</Button>
            <Button type="primary" onClick={handleConfirm}>
              确定
            </Button>
          </Space>
        }
      >
        <div style={containerStyle}>
          {/* 预览区 */}
          <div style={sectionStyle}>
            <div style={sectionLabelStyle}>预览</div>
            {renderPreview()}
          </div>

          {/* 操作按钮 */}
          <div style={sectionStyle}>
            {mode === 'normal' ? (
              <Space>
                <Button
                  icon={<EditOutlined />}
                  onClick={() => setShowVariablePicker(true)}
                >
                  插入变量
                </Button>
                <Button
                  icon={<CodeOutlined />}
                  onClick={handleSwitchToAdvanced}
                >
                  高级配置
                </Button>
              </Space>
            ) : (
              <Button
                icon={<RollbackOutlined />}
                onClick={handleResetToNormal}
              >
                重置
              </Button>
            )}
          </div>

          {/* 内容编辑区 */}
          <div style={sectionStyle}>
            <div style={sectionLabelStyle}>内容</div>
            {mode === 'normal' ? (
              <>
                <TextArea
                  value={content}
                  onChange={e => setContent(e.target.value)}
                  placeholder="点击上方按钮插入变量，或直接输入文字内容"
                  autoSize={{ minRows: 3, maxRows: 6 }}
                />
                <div style={hintStyle}>
                  {'示例：你好，${当前操作人}，有一笔${请假天数}元的审批'}
                </div>
              </>
            ) : (
              <TextArea
                value={content}
                onChange={e => setContent(e.target.value)}
                placeholder='// @TITLE\nreturn "审批：" + request.getOperatorName()'
                autoSize={{ minRows: 6, maxRows: 10 }}
                style={{ fontFamily: 'Courier New, monospace' }}
              />
            )}
          </div>
        </div>
      </Modal>

      <VariablePicker
        mappings={mappings}
        onSelect={handleInsertVariable}
        visible={showVariablePicker}
        onClose={() => setShowVariablePicker(false)}
      />
    </>
  );
};

// Styles
const containerStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 16,
};

const sectionStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 8,
};

const sectionLabelStyle: React.CSSProperties = {
  fontSize: 12,
  color: 'rgba(0, 0, 0, 0.45)',
};

const previewStyle: React.CSSProperties = {
  padding: 12,
  backgroundColor: 'rgba(0, 0, 0, 0.02)',
  borderRadius: 4,
  minHeight: 40,
  color: 'rgba(0, 0, 0, 0.65)',
};

const hintStyle: React.CSSProperties = {
  fontSize: 12,
  color: 'rgba(0, 0, 0, 0.25)',
};

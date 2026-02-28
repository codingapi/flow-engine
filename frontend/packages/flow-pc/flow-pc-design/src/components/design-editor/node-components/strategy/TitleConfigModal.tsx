import React, { useState, useEffect, useRef, useMemo } from 'react';
import { Modal, Input, Alert, Button, Space, message, Popover, Empty } from 'antd';
import { EditOutlined, CodeOutlined, RollbackOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import { GroovyVariableMapping } from '@flow-engine/flow-types';
import { GroovyVariableService } from '@/services/groovy-variable-service';
import { TitleSyntaxConverter } from '@/utils/title-syntax-converter';

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
  const [cursorPosition, setCursorPosition] = useState(0);
  const [variablePickerOpen, setVariablePickerOpen] = useState(false);
  const textareaRef = useRef<any>(null);

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

  // 插入变量到光标位置
  const handleInsertVariable = (mapping: GroovyVariableMapping) => {
    const variableText = `\${${mapping.label}}`;
    // 使用之前保存的光标位置插入变量
    const start = cursorPosition;
    const newContent = content.substring(0, start) + variableText + content.substring(start);
    setContent(newContent);
    // 设置光标位置到插入变量之后
    setCursorPosition(start + variableText.length);
    // 聚焦到 textarea
    setTimeout(() => {
      if (textareaRef.current) {
        textareaRef.current.focus();
        textareaRef.current.setSelectionRange(start + variableText.length, start + variableText.length);
      }
    }, 0);
  };

  // 处理文本框变化时更新光标位置
  const handleContentChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setContent(e.target.value);
    setCursorPosition(e.target.selectionStart || 0);
  };

  // 处理文本框聚焦时更新光标位置
  const handleTextareaFocus = (e: React.FocusEvent<HTMLTextAreaElement>) => {
    setCursorPosition(e.target.selectionStart || 0);
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

  // 切换回普通模式
  const handleSwitchToNormal = () => {
    userModifiedModeRef.current = true;
    // 尝试解析当前高级脚本为标签表达式
    const labelExpr = TitleSyntaxConverter.toLabelExpression(content, mappings);
    if (labelExpr !== null) {
      // 能解析，切换到普通模式并显示解析后的内容
      setContent(labelExpr);
      setMode('normal');
    } else {
      // 无法解析，提示用户确认
      Modal.confirm({
        title: '切换回普通模式',
        content: '当前自定义代码无法解析为可视化标签表达式，切换后将丢失这些配置。是否继续？',
        okText: '确定',
        cancelText: '取消',
        onOk: () => {
          setContent('');
          setMode('normal');
        },
      });
    }
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
                <Popover
                  content={
                    <VariablePickerContent
                      mappings={mappings}
                      onSelect={handleInsertVariable}
                    />
                  }
                  trigger="click"
                  placement="bottomLeft"
                  open={variablePickerOpen}
                  onOpenChange={setVariablePickerOpen}
                  overlayStyle={{ padding: 0 }}
                  getPopupContainer={(trigger) => trigger.parentElement || document.body}
                >
                  <Button
                    icon={<EditOutlined />}
                    onClick={() => setVariablePickerOpen(true)}
                  >
                    插入变量
                  </Button>
                </Popover>
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
                onClick={handleSwitchToNormal}
              >
                返回普通模式
              </Button>
            )}
          </div>

          {/* 内容编辑区 */}
          <div style={sectionStyle}>
            <div style={sectionLabelStyle}>内容</div>
            {mode === 'normal' ? (
              <>
                <TextArea
                  ref={textareaRef}
                  value={content}
                  onChange={handleContentChange}
                  onFocus={handleTextareaFocus}
                  placeholder="点击上方按钮插入变量，或直接输入文字内容"
                  autoSize={{ minRows: 3, maxRows: 6 }}
                />
                <div style={hintStyle}>
                  {'示例：你好，${当前操作人}，有一笔${请假天数}元的审批'}
                </div>
              </>
            ) : (
              <TextArea
                ref={textareaRef}
                value={content}
                onChange={handleContentChange}
                onFocus={handleTextareaFocus}
                placeholder='// @CUSTOM_SCRIPT\ndef run(request){\n    return "审批：" + request.getOperatorName()\n}'
                autoSize={{ minRows: 6, maxRows: 10 }}
                style={{ fontFamily: 'Courier New, monospace' }}
              />
            )}
          </div>

          {/* 底部操作区 */}
          <div style={footerStyle}>
            <Button
              danger
              icon={<DeleteOutlined />}
              onClick={() => {
                Modal.confirm({
                  title: '重置设置',
                  content: '确定要清除所有标题配置吗？',
                  okText: '确定',
                  cancelText: '取消',
                  onOk: () => {
                    userModifiedModeRef.current = true;
                    setContent('');
                    setMode('normal');
                  },
                });
              }}
            >
              重置设置
            </Button>
          </div>
        </div>
      </Modal>
    </>
  );
};

// Popover 版本的变量选择器内容
const VariablePickerContent: React.FC<{
  mappings: GroovyVariableMapping[];
  onSelect: (mapping: GroovyVariableMapping) => void;
}> = ({ mappings, onSelect }) => {
  const [searchText, setSearchText] = useState('');

  // 阻止事件冒泡，防止 Popover 关闭
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    e.stopPropagation();
    setSearchText(e.target.value);
  };

  const filteredMappings = useMemo(() => {
    if (!searchText) {
      return mappings;
    }
    const lowerSearch = searchText.toLowerCase();
    return mappings.filter(
      m =>
        m.label.toLowerCase().includes(lowerSearch) ||
        m.value.toLowerCase().includes(lowerSearch)
    );
  }, [mappings, searchText]);

  const groupedMappings = useMemo(() => {
    return GroovyVariableService.groupByTag(filteredMappings);
  }, [filteredMappings]);

  return (
    <div style={pickerContainerStyle} onClick={e => e.stopPropagation()}>
      <Input
        placeholder="搜索变量..."
        prefix={<SearchOutlined style={{ color: '#999' }} />}
        value={searchText}
        onChange={handleInputChange}
        allowClear
        style={{ marginBottom: 8 }}
      />
      <div style={pickerListStyle}>
        {groupedMappings.size === 0 ? (
          <Empty description="未找到匹配的变量" image={Empty.PRESENTED_IMAGE_SIMPLE} />
        ) : (
          Array.from(groupedMappings.entries()).map(([tag, variables]) => (
            <div key={tag}>
              <div style={pickerGroupTitleStyle}>{tag}</div>
              <div style={pickerItemsStyle}>
                {variables.map(variable => (
                  <div
                    key={variable.label}
                    style={pickerItemStyle}
                    onClick={() => onSelect(variable)}
                  >
                    <span style={pickerItemLabelStyle}>{variable.label}</span>
                    <span style={pickerItemValueStyle}>{variable.value}</span>
                  </div>
                ))}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
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

const footerStyle: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'flex-start',
  paddingTop: 8,
  borderTop: '1px solid #f0f0f0',
};

const pickerContainerStyle: React.CSSProperties = {
  width: 400,
  maxHeight: 300,
  overflow: 'auto',
};

const pickerListStyle: React.CSSProperties = {
  maxHeight: 250,
  overflow: 'auto',
};

const pickerGroupTitleStyle: React.CSSProperties = {
  fontSize: 12,
  fontWeight: 500,
  color: 'rgba(0, 0, 0, 0.45)',
  padding: '8px 0 4px',
};

const pickerItemsStyle: React.CSSProperties = {
  display: 'flex',
  flexWrap: 'wrap',
  gap: 8,
};

const pickerItemStyle: React.CSSProperties = {
  padding: '4px 8px',
  backgroundColor: '#f5f5f5',
  borderRadius: 4,
  cursor: 'pointer',
  display: 'flex',
  flexDirection: 'column',
  gap: 2,
};

const pickerItemLabelStyle: React.CSSProperties = {
  fontSize: 13,
  color: 'rgba(0, 0, 0, 0.88)',
};

const pickerItemValueStyle: React.CSSProperties = {
  fontSize: 11,
  color: 'rgba(0, 0, 0, 0.45)',
};

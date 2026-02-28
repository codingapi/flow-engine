import React, { useState, useEffect, useRef } from 'react';
import { Modal, Input, Alert, Button, Space, message } from 'antd';
import { EditOutlined, CodeOutlined } from '@ant-design/icons';
import { GroovyVariableMapping } from '@flow-engine/flow-types';
import { ScriptConfigModal, ScriptConfigModalProps } from './components/script-config-modal';
import { ScriptType } from '@/components/design-editor/typings/groovy-script';
import { groovyVariableService } from '@/components/design-editor/script/service/groovy-variable-service';

export interface NodeTitleConfigModalProps extends Omit<ScriptConfigModalProps, 'scriptType' | 'title' | 'variables'> {
  /** 表单字段（用于动态生成变量） */
  formFields?: Array<{ name: string; code: string }>;
}

/**
 * 节点标题配置弹框
 * 使用通用脚本配置弹框，支持普通模式和高级模式
 */
export const NodeTitleConfigModal: React.FC<NodeTitleConfigModalProps> = ({
  script,
  formFields,
  onConfirm,
  onCancel,
}) => {
  // 获取系统变量，并合并表单字段
  const variables = groovyVariableService.getVariables(ScriptType.TITLE, { formFields });

  return (
    <ScriptConfigModal
      scriptType={ScriptType.TITLE}
      script={script}
      variables={variables}
      onConfirm={onConfirm}
      onCancel={onCancel}
      title="标题配置"
    />
  );
};

export default NodeTitleConfigModal;

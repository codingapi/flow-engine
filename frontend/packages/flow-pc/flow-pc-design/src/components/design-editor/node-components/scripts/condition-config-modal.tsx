import React from 'react';
import { GroovyVariableMapping } from '@flow-engine/flow-types';
import { ScriptConfigModal, ScriptConfigModalProps } from './components/script-config-modal';
import { ScriptType } from '@/components/design-editor/typings/groovy-script';
import { GroovyVariableService } from '@/components/design-editor/script/service/groovy-variable-service';

export interface ConditionConfigModalProps extends Omit<ScriptConfigModalProps, 'scriptType' | 'title'> {
  /** 额外的变量（可选） */
  extraVariables?: GroovyVariableMapping[];
}

/**
 * 条件配置弹框
 * 使用通用脚本配置弹框，支持普通模式和高级模式
 */
export const ConditionConfigModal: React.FC<ConditionConfigModalProps> = ({
  script,
  variables,
  onConfirm,
  onCancel,
  extraVariables = [],
}) => {
  // 获取系统变量
  const systemVariables = GroovyVariableService.getSystemVariables(ScriptType.CONDITION);

  // 合并变量
  const allVariables = [...systemVariables, ...extraVariables];

  return (
    <ScriptConfigModal
      scriptType={ScriptType.CONDITION}
      script={script}
      variables={allVariables}
      onConfirm={onConfirm}
      onCancel={onCancel}
      title="条件配置"
    />
  );
};

export default ConditionConfigModal;

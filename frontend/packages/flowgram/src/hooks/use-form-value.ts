/**
 * Copyright (c) 2025 Bytedance Ltd. and/or its affiliates
 * SPDX-License-Identifier: MIT
 */

import { useEffect, useState } from 'react';

import { FlowNodeEntity, FlowNodeFormData, FormModelV2 } from '@flowgram.ai/fixed-layout-editor';

export const useFormValue = <T = unknown>(params: {
  node?: FlowNodeEntity;
  fieldName: string;
  defaultValue?: T;
}): [T, (v: T) => void] => {
  const { node, fieldName, defaultValue } = params;
  const formModel = node?.getData(FlowNodeFormData).getFormModel<FormModelV2>();

  const [innerValue, setInnerValue] = useState<T | undefined>(() =>
    formModel?.getValueIn<T>(fieldName)
  );

  // 初始化表单值
  useEffect(() => {
    if (!formModel) {
      return;
    }
    const initValue = formModel.getValueIn<{ width: number; height: number }>(fieldName);
    if (!initValue) {
      formModel.setValueIn(fieldName, defaultValue);
    }
  }, [defaultValue, formModel, fieldName]);

  // 同步表单外部值变化：初始化/undo/redo/协同
  useEffect(() => {
    if (!formModel) {
      return;
    }
    const disposer = formModel.onFormValueChangeIn(fieldName, () => {
      const newValue = formModel.getValueIn<T>(fieldName);
      if (!newValue) {
        return;
      }
      setInnerValue(newValue);
    });
    return () => disposer.dispose();
  }, [formModel, fieldName]);

  const setValue = (newValue: T) => {
    if (!formModel) {
      return;
    }
    formModel.setValueIn(fieldName, newValue);
    setInnerValue(newValue);
  };

  const value = (innerValue ?? defaultValue) as T;
  return [value, setValue];
};

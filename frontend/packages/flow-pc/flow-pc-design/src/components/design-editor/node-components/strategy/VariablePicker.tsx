import React, { useMemo, useState } from 'react';
import { Modal, Input, Empty } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { GroovyVariableMapping } from '@flow-engine/flow-types';
import { GroovyVariableService } from '@/services/groovy-variable-service';
import styles from './VariablePicker.module.less';

export interface VariablePickerProps {
  /** 变量映射列表 */
  mappings: GroovyVariableMapping[];
  /** 选中变量回调 */
  onSelect: (mapping: GroovyVariableMapping) => void;
  /** 是否显示 */
  visible: boolean;
  /** 关闭回调 */
  onClose: () => void;
}

/**
 * 变量选择器组件
 * 用于在标题表达式中插入变量
 */
export const VariablePicker: React.FC<VariablePickerProps> = ({
  mappings,
  onSelect,
  visible,
  onClose,
}) => {
  const [searchText, setSearchText] = useState('');

  // 过滤变量
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

  // 按tag分组
  const groupedMappings = useMemo(() => {
    return GroovyVariableService.groupByTag(filteredMappings);
  }, [filteredMappings]);

  const handleVariableClick = (mapping: GroovyVariableMapping) => {
    onSelect(mapping);
    onClose();
    setSearchText('');
  };

  return (
    <Modal
      title="选择变量"
      open={visible}
      onCancel={onClose}
      footer={null}
      width={600}
      className={styles.variablePicker}
    >
      <div className={styles.searchWrapper}>
        <Input
          placeholder="搜索变量..."
          prefix={<SearchOutlined />}
          value={searchText}
          onChange={e => setSearchText(e.target.value)}
          allowClear
        />
      </div>

      <div className={styles.variableList}>
        {groupedMappings.size === 0 ? (
          <Empty description="未找到匹配的变量" />
        ) : (
          Array.from(groupedMappings.entries()).map(([tag, variables]) => (
            <div key={tag} className={styles.variableGroup}>
              <div className={styles.groupTitle}>{tag}</div>
              <div className={styles.variableItems}>
                {variables.map(variable => (
                  <div
                    key={variable.label}
                    className={styles.variableItem}
                    onClick={() => handleVariableClick(variable)}
                  >
                    <div className={styles.variableLabel}>{variable.label}</div>
                    <div className={styles.variableValue}>{variable.value}</div>
                  </div>
                ))}
              </div>
            </div>
          ))
        )}
      </div>
    </Modal>
  );
};

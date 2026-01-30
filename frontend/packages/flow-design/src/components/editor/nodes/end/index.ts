import { FlowNodeRegistry } from '../../typings';
import { formMeta } from './form-meta';

export const EndNodeRegistry: FlowNodeRegistry = {
  type: 'END',
  meta: {
      isNodeEnd: true, // Mark as end
      selectable: false, // End node cannot select
      copyDisable: true, // End node canot copy
      expandable: false, // disable expanded
      addDisable: true, // Start Node cannot be added
  },
  info: {
    icon: 'END',
    description: '结束节点',
  },
  /**
   * Render node via formMeta
   */
  formMeta,
};

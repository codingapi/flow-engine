import { FlowNodeRegistry } from '../../typings';
import { formMeta } from './form-meta';

export const RouterNodeRegistry: FlowNodeRegistry = {
  type: 'ROUTER',
  meta: {
      isNodeEnd: true, // Mark as end
      selectable: false, // End node cannot select
      copyDisable: true, // End node canot copy
      expandable: false, // disable expanded
      addDisable: false, // Start Node cannot be added
  },
  info: {
    icon: 'ROUTER',
    description: '路由节点',
  },
  /**
   * Render node via formMeta
   */
  formMeta,
};

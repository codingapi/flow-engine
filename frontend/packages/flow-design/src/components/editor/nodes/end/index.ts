import { FlowNodeRegistry } from '../../typings';
import iconEnd from '@/assets/icon-end.jpg';
import { formMeta } from './form-meta';

export const EndNodeRegistry: FlowNodeRegistry = {
  type: 'end',
  meta: {
      isNodeEnd: true, // Mark as end
      selectable: false, // End node cannot select
      copyDisable: true, // End node canot copy
      expandable: false, // disable expanded
      addDisable: true, // Start Node cannot be added
  },
  info: {
    icon: iconEnd,
    description:
      'The starting node of the workflow, used to set the information needed to initiate the workflow.',
  },
  /**
   * Render node via formMeta
   */
  formMeta,
};

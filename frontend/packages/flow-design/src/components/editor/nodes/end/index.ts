import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';

export const EndNodeRegistry: FlowNodeRegistry = {
    type: 'END',
    extend:'end',
    meta: {
        isNodeEnd: true, // Mark as end
        deleteDisable:true, // End node cannot delete
        selectable: false, // End node cannot select
        copyDisable: true, // End node canot copy
        expandable: false, // disable expanded
        addDisable: true, // End Node cannot be added,
        sidebarDisable: true, // End Node cannot be added from sidebar
        style:{
            width: '100%',
        }
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

import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';

export const StartNodeRegistry: FlowNodeRegistry = {
    type: 'START',
    meta: {
        isStart: true, // Mark as start
        deleteDisable: true, // Start node cannot delete
        selectable: false, // Start node cannot select
        copyDisable: true, // Start node cannot copy
        expandable: false, // disable expanded
        addDisable: true, // Start Node cannot be added
        editTitleDisable: true, // Start Node title cannot be edited,
    },
    info: {
        icon: 'START',
        description: '发起节点',
    },
    formMeta,
};

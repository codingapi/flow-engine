import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';

export const ApprovalNodeRegistry: FlowNodeRegistry = {
    type: 'APPROVAL',
    meta: {
        copyDisable: true,
        addDisable: false,
        expandable: false, // disable expanded,
    },
    info: {
        icon: 'APPROVAL',
        description: '审批节点',
    },
    /**
     * Render node via formMeta
     */
    formMeta
};

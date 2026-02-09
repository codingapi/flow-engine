import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';

export const HandleNodeRegistry: FlowNodeRegistry = {
    type: 'HANDLE',
    meta: {
        copyDisable: true,
        addDisable: false,
        expandable: false, // disable expanded,
    },
    info: {
        icon: 'HANDLE',
        description: '办理节点',
    },
    /**
     * Render node via formMeta
     */
    formMeta,
};

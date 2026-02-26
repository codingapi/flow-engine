import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';

export const NotifyNodeRegistry: FlowNodeRegistry = {
    type: 'NOTIFY',
    meta: {
        copyDisable: true,
        addDisable: false,
        expandable: false, // disable expanded
    },
    info: {
        icon: 'NOTIFY',
        description: '抄送节点',
    },
    /**
     * Render node via formMeta
     */
    formMeta
};

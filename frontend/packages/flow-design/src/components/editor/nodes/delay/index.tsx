import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';

export const DelayNodeRegistry: FlowNodeRegistry = {
    type: 'DELAY',
    meta: {
        copyDisable: true,
        addDisable: false,
        expandable: false, // disable expanded
    },
    info: {
        icon: 'DELAY',
        description: '延迟节点',
    },
    /**
     * Render node via formMeta
     */
    formMeta
};

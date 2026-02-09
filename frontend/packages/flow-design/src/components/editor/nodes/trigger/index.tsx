import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';

export const TriggerNodeRegistry: FlowNodeRegistry = {
    type: 'TRIGGER',
    meta: {
        copyDisable: true,
        addDisable: false,
        expandable: false, // disable expanded
    },
    info: {
        icon: 'TRIGGER',
        description: '触发节点',
    },
    /**
     * Render node via formMeta
     */
    formMeta
};

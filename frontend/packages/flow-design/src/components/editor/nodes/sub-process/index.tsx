import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';
import {nanoid} from "nanoid";

export const SubProcessNodeRegistry: FlowNodeRegistry = {
    type: 'SUB_PROCESS',
    meta: {
        copyDisable: true,
        addDisable: false,
        expandable: false, // disable expanded
    },
    info: {
        icon: 'SUB_PROCESS',
        description: '子流程节点',
    },
    /**
     * Render node via formMeta
     */
    formMeta
};

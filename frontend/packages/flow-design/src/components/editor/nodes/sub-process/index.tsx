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
    formMeta,
    onAdd: (ctx, from) => {
        return {
            id: `sub_process_${nanoid()}`,
            type: 'SUB_PROCESS',
            data: {
                title: `子流程节点`,
                value: 'SUB_PROCESS Value'
            },
        }
    }
};

import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';
import {nanoid} from "nanoid";

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
    formMeta,
    onAdd: (ctx, from) => {
        return {
            id: `trigger_${nanoid()}`,
            type: 'TRIGGER',
            data: {
                title: `TRIGGER Node`,
                value: 'TRIGGER Value'
            },
        }
    }
};

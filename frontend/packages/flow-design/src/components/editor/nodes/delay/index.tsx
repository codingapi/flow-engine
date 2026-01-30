import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';
import {nanoid} from "nanoid";

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
    formMeta,
    onAdd: (ctx, from) => {
        return {
            id: `delay_${nanoid()}`,
            type: 'DELAY',
            data: {
                title: `延迟节点`,
                value: 'DELAY Value'
            },
        }
    }
};

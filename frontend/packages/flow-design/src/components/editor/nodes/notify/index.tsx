import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';
import {nanoid} from "nanoid";

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
    formMeta,
    onAdd: (ctx, from) => {
        return {
            id: `notify_${nanoid()}`,
            type: 'NOTIFY',
            data: {
                title: `抄送节点`,
                value: 'NOTIFY Value'
            },
        }
    }
};

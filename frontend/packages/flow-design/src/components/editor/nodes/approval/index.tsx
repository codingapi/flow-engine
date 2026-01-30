import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';
import {nanoid} from "nanoid";

export const ApprovalNodeRegistry: FlowNodeRegistry = {
    type: 'APPROVAL',
    meta: {
        copyDisable: true,
        addDisable: false,
        expandable: false, // disable expanded
    },
    info: {
        icon: 'APPROVAL',
        description: '审批节点',
    },
    /**
     * Render node via formMeta
     */
    formMeta,
    onAdd: (ctx, from) => {
        return {
            id: `approval_${nanoid()}`,
            type: 'APPROVAL',
            data: {
                title: `审批节点`,
                value: 'APPROVAL Value'
            },
        }
    }
};

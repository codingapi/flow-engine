import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';
import {nanoid} from "nanoid";

export const ConditionBranchNodeRegistry: FlowNodeRegistry = {
    type: 'CONDITION_BRANCH',
    extend: 'block',
    meta: {
        copyDisable: true,
        addDisable: true,
        strategies:[
            'ConditionBranchStrategy'
        ]
    },
    info: {
        icon: 'CONDITION_BRANCH',
        description: '分支节点',
    },
    /**
     * Render node via formMeta
     */
    formMeta,
    onAdd(ctx, from) {
        return {
            id: `condition_branch_${nanoid(5)}`,
            type: 'CONDITION_BRANCH',
            data: {
                title: `条件分支节点`,
                value: 'branch Value'
            },
        };
    }
};

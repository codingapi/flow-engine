import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';
import {nanoid} from "nanoid";

export const InclusiveBranchNodeRegistry: FlowNodeRegistry = {
    type: 'INCLUSIVE_BRANCH',
    extend: 'block',
    meta: {
        copyDisable: true,
        addDisable: true,
    },
    info: {
        icon: 'INCLUSIVE_BRANCH',
        description: '包容分支',
    },
    /**
     * Render node via formMeta
     */
    formMeta,
    onAdd(ctx, from) {
        return {
            id: `inclusive_branch_${nanoid(5)}`,
            type: 'INCLUSIVE_BRANCH',
            data: {
                title: `包容分支节点`,
                value: 'INCLUSIVE_BRANCH Value'
            },
        };
    }
};

import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';
import {nanoid} from "nanoid";

export const ParallelBranchNodeRegistry: FlowNodeRegistry = {
    type: 'PARALLEL_BRANCH',
    extend: 'block',
    meta: {
        copyDisable: true,
        addDisable: true,
        strategies:[
            'ParallelBranchStrategy'
        ]
    },
    info: {
        icon: 'PARALLEL_BRANCH',
        description: '并行分支',
    },
    /**
     * Render node via formMeta
     */
    formMeta,
    onAdd(ctx, from) {
        return {
            id: `parallel_branch_${nanoid(5)}`,
            type: 'PARALLEL_BRANCH',
            data: {
                title: `并行分支节点`,
                value: 'branch Value'
            },
        };
    }
};

import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';
import {FlowNodeSplitType} from "@flowgram.ai/fixed-layout-editor";
import {nanoid} from "nanoid";

export const ParallelNodeRegistry: FlowNodeRegistry = {
    type: 'PARALLEL',
    extend: FlowNodeSplitType.DYNAMIC_SPLIT,
    meta: {
        copyDisable: true,
        addDisable: false,
        expandable: false, // disable expanded
    },
    info: {
        icon: 'PARALLEL',
        description: '并行控制',
    },
    /**
     * Render node via formMeta
     */
    formMeta,
    onAdd: (ctx, from) => {
        return {
            id: `parallel_${nanoid()}`,
            type: 'PARALLEL',
            data: {
                title: `PARALLEL Node`,
                value: 'PARALLEL Value'
            },
            blocks: [
                {
                    id: `parallel_branch_${nanoid(5)}`,
                    type: 'PARALLEL_BRANCH',
                    data: {
                        title: `branch title`,
                        value: 'branch Value'
                    },
                },
                {
                    id: `parallel_branch_${nanoid(5)}`,
                    type: 'PARALLEL_BRANCH',
                    data: {
                        title: `branch title`,
                        value: 'branch Value'
                    },
                }
            ]
        }
    }
};

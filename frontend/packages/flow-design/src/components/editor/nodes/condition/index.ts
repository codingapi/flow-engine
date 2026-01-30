import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';
import {FlowNodeSplitType} from "@flowgram.ai/fixed-layout-editor";
import {nanoid} from "nanoid";

export const ConditionNodeRegistry: FlowNodeRegistry = {
    type: 'CONDITION',
    extend: FlowNodeSplitType.DYNAMIC_SPLIT,
    meta: {
        copyDisable: true,
        addDisable: false,
        expandable: false, // disable expanded
    },
    info: {
        icon: 'CONDITION',
        description: '分支控制',
    },
    /**
     * Render node via formMeta
     */
    formMeta,
    onAdd: (ctx, from) => {
        return {
            id: `condition_${nanoid()}`,
            type: 'CONDITION',
            data: {
                title: `CONDITION Node`,
                value: 'CONDITION Value'
            },
            blocks: [
                {
                    id: `condition_branch_${nanoid(5)}`,
                    type: 'CONDITION_BRANCH',
                    data: {
                        title: `branch title`,
                        value: 'branch Value'
                    },
                },
                {
                    id: `condition_branch_${nanoid(5)}`,
                    type: 'CONDITION_BRANCH',
                    data: {
                        title: `branch title`,
                        value: 'branch Value'
                    },
                }
            ]
        }
    }
};

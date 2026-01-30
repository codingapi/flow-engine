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
        sidebarDisable: true, // End Node cannot be added from sidebar
        style:{
            width: '100%',
        }
    },
    info: {
        icon: 'CONDITION',
        description: '条件控制',
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
                title: `条件控制`,
            },
            blocks: [
                {
                    id: `condition_branch_${nanoid(5)}`,
                    type: 'CONDITION_BRANCH',
                    data: {
                        title: `条件分支节点`,
                        value: 'branch Value'
                    },
                },
                {
                    id: `condition_branch_${nanoid(5)}`,
                    type: 'CONDITION_BRANCH',
                    data: {
                        title: `条件分支节点`,
                        value: 'branch Value'
                    },
                }
            ]
        }
    }
};

import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';
import {FlowNodeSplitType} from "@flowgram.ai/fixed-layout-editor";
import {nanoid} from "nanoid";

export const InclusiveNodeRegistry: FlowNodeRegistry = {
    type: 'INCLUSIVE',
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
        icon: 'INCLUSIVE',
        description: '包容控制',
    },
    /**
     * Render node via formMeta
     */
    formMeta,
    onAdd: (ctx, from) => {
        return {
            id: `inclusive_${nanoid()}`,
            type: 'INCLUSIVE',
            data: {
                title: `包容分支`,
            },
            blocks:[
                {
                    id: `inclusive_branch_${nanoid(5)}`,
                    type: 'INCLUSIVE_BRANCH',
                    data: {
                        title: `包容分支节点`,
                        value: 'INCLUSIVE_BRANCH Value'
                    },
                },
                {
                    id: `inclusive_branch_${nanoid(5)}`,
                    type: 'INCLUSIVE_BRANCH',
                    data: {
                        title: `包容分支节点`,
                        value: 'INCLUSIVE_BRANCH Value'
                    },
                }
            ]
        }
    }
};

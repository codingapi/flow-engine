import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';
import {FlowNodeSplitType} from "@flowgram.ai/fixed-layout-editor";

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
    formMeta
};

import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';
import {FlowNodeSplitType} from "@flowgram.ai/fixed-layout-editor";

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
};

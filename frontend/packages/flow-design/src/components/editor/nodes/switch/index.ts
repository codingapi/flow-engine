import {FlowNodeRegistry} from '../../typings';
import iconStart from '@/assets/icon-start.jpg';
import {formMeta} from './form-meta';
import { FlowNodeSplitType } from "@flowgram.ai/fixed-layout-editor";
import {nanoid} from "nanoid";

export const SwitchNodeRegistry: FlowNodeRegistry = {
    type: 'switch',
    extend: FlowNodeSplitType.DYNAMIC_SPLIT,
    meta: {
        copyDisable: true,
        addDisable: false,
        expandable: false, // disable expanded
    },
    info: {
        icon: iconStart,
        description:
            'The starting node of the workflow, used to set the information needed to initiate the workflow.',
    },
    /**
     * Render node via formMeta
     */
    formMeta,
    onAdd: (ctx, from) => {
        return {
            id: `switch_${nanoid()}`,
            type: 'switch',
            data: {
                title: `Switch Node`,
                value: 'Switch Value'
            },
            blocks: []
        }
    }
};

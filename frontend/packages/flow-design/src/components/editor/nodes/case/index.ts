import {FlowNodeRegistry} from '../../typings';
import iconStart from '@/assets/icon-start.jpg';
import {formMeta} from './form-meta';
import {nanoid} from "nanoid";

export const CaseNodeRegistry: FlowNodeRegistry = {
    type: 'case',
    extend: 'block',
    meta: {
        copyDisable: true,
        addDisable: true,
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
    onAdd(ctx, from) {
        return {
            id: `Case_${nanoid(5)}`,
            type: 'case',
            data: {
                title: `Case_title`,
                value: 'Case Value'
            },
        };
    }
};

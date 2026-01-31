import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';
import {nanoid} from "nanoid";

export const HandleNodeRegistry: FlowNodeRegistry = {
    type: 'HANDLE',
    meta: {
        copyDisable: true,
        addDisable: false,
        expandable: false, // disable expanded,
        strategies:[
            'TimeoutStrategy',
            'MultiOperatorAuditStrategy',
            'SameOperatorAuditStrategy',
            'RecordMergeStrategy',
            'ResubmitStrategy',
            'AdviceStrategy',
            'ErrorTriggerStrategy',
            'NodeTitleStrategy',
            'FormFieldPermissionStrategy',
            'OperatorLoadStrategy',
        ]
    },
    info: {
        icon: 'HANDLE',
        description: '办理节点',
    },
    /**
     * Render node via formMeta
     */
    formMeta,
    onAdd: (ctx, from) => {
        return {
            id: `handle_${nanoid()}`,
            type: 'HANDLE',
            data: {
                title: `办理节点`,
                value: 'HANDLE Value'
            },
        }
    }
};

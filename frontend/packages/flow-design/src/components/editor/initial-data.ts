import type { FlowDocumentJSON } from '@flowgram.ai/fixed-layout-editor';

export const initialData: FlowDocumentJSON = {
    nodes: [
        {
            id: 'start',
            type: 'START',
            data:{
                title:'开始节点',
                value:'start-value'
            }
        },
        {
            id: 'approval_1',
            type: 'APPROVAL',
            data:{
                title:'审批节点',
                value:'APPROVAL value'
            },
        },
        {
            id: 'condition_1',
            type: 'CONDITION',
            data:{
                title:'分支控制',
                value:'condition value'
            },
            blocks:[
                {
                    id: 'case_1',
                    type: 'CONDITION_BRANCH',
                    data:{
                        title:'条件节点A',
                        value:'condition-branch-value-a'
                    },
                },
                {
                    id: 'case_2',
                    type: 'CONDITION_BRANCH',
                    data:{
                        title:'条件节点B',
                        value:'condition-branch-value-b'
                    },
                }
            ]
        },
        {
            id: 'end',
            type: 'END',
            data:{
                title:'结束节点',
                value:'end-value'
            }
        },
    ],
};

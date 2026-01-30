import type { FlowDocumentJSON } from '@flowgram.ai/fixed-layout-editor';

export const initialData: FlowDocumentJSON = {
    nodes: [
        {
            id: 'start',
            type: 'start',
            data:{
                title:'开始节点',
                value:'start-value'
            }
        },
        {
            id: 'switch_1',
            type: 'switch',
            data:{
                title:'switch节点',
                value:'switch-value'
            },
            blocks:[
                {
                    id: 'case_1',
                    type: 'case',
                    data:{
                        title:'条件节点A',
                        value:'case-value-a'
                    },
                },
                {
                    id: 'case_2',
                    type: 'case',
                    data:{
                        title:'条件节点B',
                        value:'case-value-b'
                    },
                }
            ]
        },
        {
            id: 'end',
            type: 'end',
            data:{
                title:'结束节点',
                value:'end-value'
            }
        },
    ],
};

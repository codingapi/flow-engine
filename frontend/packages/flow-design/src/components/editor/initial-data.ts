import type { FlowDocumentJSON } from '@flowgram.ai/fixed-layout-editor';

export const initialData: FlowDocumentJSON = {
    nodes: [
        {
            id: 'start_0',
            type: 'start',
            data:{
                title:'start-title',
                value:'start-value'
            }
        },
        {
            id: 'custom_1',
            type: 'custom',
            data:{
                title:'custom-title',
                value:'custom-value'
            }
        },
        {
            id: 'end_2',
            type: 'end',
            data:{
                title:'end-title',
                value:'end-value'
            }
        },
    ],
};

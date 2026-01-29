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
            id: 'custom_1',
            type: 'custom',
            data:{
                title:'自定义节点',
                value:'custom-value'
            }
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

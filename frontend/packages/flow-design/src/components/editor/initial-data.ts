import {FlowDocumentJSON} from "@/components/editor/typings";

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
            id: 'end',
            type: 'END',
            data:{
                title:'结束节点',
                value:'end-value'
            }
        },
    ],
};

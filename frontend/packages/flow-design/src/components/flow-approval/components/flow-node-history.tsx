import React from "react";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";
import {ProcessNode} from "@/components/flow-approval/typings";

export interface FlowNodeHistoryAction{
    refresh:()=>void;
}

interface FlowNodeHistoryProps{
    actionRef?:React.Ref<FlowNodeHistoryAction>;
}

export const FlowNodeHistory: React.FC<FlowNodeHistoryProps> = (props) => {

    const {context} = useApprovalContext();

    const [processNodes, setProcessNodes] = React.useState<ProcessNode[]>([]);

    const triggerProcessNodes = () => {
        context.getPresenter().processNodes().then(nodes => {
            setProcessNodes(nodes);
        });
    }

    React.useEffect(()=>{
        triggerProcessNodes();
    },[]);

    React.useImperativeHandle(props.actionRef,()=>{
        return {
            refresh:()=>{
                triggerProcessNodes();
            }
        }
    },[]);

    return (
        <div>
            流转历史 {processNodes.length}
        </div>
    )
}
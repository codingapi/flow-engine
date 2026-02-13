import React from "react";
import {Drawer} from "@/components/drawer";
import {detail} from "@/api/record";
import {FlowContent} from "@/components/approval/typings";
import {ApprovalLayout} from "@/components/approval/layout";


interface ApprovalPanelProps {
    workflowCode?: string;
    recordId?:string;
    onClose?: () => void;
}

export const ApprovalPanel: React.FC<ApprovalPanelProps> = (props) => {

    const [state,dispatch] = React.useState<FlowContent|undefined>(undefined);

    React.useEffect(()=>{
        const id = props.workflowCode || props.recordId || '';
        detail(id).then(res=>{
            if(res.success){
                dispatch(res.data);
            }
        });
    },[]);

    return (
        <>
            {state && <ApprovalLayout content={state} onClose={props.onClose}/>}
        </>
    )
}

interface ApprovalPanelDrawerProps extends ApprovalPanelProps {
    open: boolean;
    onClose: () => void;
}

export const ApprovalPanelDrawer: React.FC<ApprovalPanelDrawerProps> = (props) => {

    return (
        <Drawer
            open={props.open}
            onClose={props.onClose}
        >
            <ApprovalPanel
                {...props}
            />
        </Drawer>
    )
}
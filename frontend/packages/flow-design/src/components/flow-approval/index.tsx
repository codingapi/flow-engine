import React from "react";
import {Drawer} from "@/components/ui/drawer";
import {detail} from "@/api/record";
import {FlowContent} from "@/components/flow-approval/typings";
import {ApprovalLayout} from "@/components/flow-approval/layout";

interface ApprovalPanelProps {
    workflowCode?: string;
    recordId?:string;
    onClose?: () => void;
}

export const ApprovalPanel: React.FC<ApprovalPanelProps> = (props) => {

    const [content,dispatch] = React.useState<FlowContent|undefined>(undefined);

    React.useEffect(()=>{
        const id =   props.recordId || props.workflowCode || '';
        detail(id).then(res=>{
            if(res.success){
                dispatch(res.data);
            }
        });
    },[]);

    return (
        <>
            {content && <ApprovalLayout content={content} onClose={props.onClose}/>}
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
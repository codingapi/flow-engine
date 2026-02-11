import React from "react";
import {Drawer} from "@/components/drawer";
import {detail} from "@/api/record";


interface ApprovalPanelProps {
    workflowCode?: string;
    recordId?:string
}

export const ApprovalPanel: React.FC<ApprovalPanelProps> = (props) => {

    React.useEffect(()=>{
        const id = props.workflowCode || props.recordId || '';
        detail(id).then(res=>{
            if(res.success){
                console.log(res.data)
            }
        });
    },[]);

    return (
        <>approval</>
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
            title={"发起流程"}
            closeIcon={true}
        >
            <ApprovalPanel
                {...props}
            />
        </Drawer>
    )
}
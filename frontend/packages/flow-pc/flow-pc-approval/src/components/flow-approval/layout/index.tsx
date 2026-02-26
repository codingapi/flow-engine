import React from "react";
import {ApprovalLayoutProps} from "../typings";
import {Provider} from "react-redux";
import {approvalStore} from "@/components/flow-approval/store";
import {ApprovalContext} from "@/components/flow-approval/context";
import {createApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";
import {Header} from "@/components/flow-approval/layout/header";
import {Body} from "@/components/flow-approval/layout/body";

const ApprovalLayoutScope: React.FC<ApprovalLayoutProps> = (props) => {
    const {context} = createApprovalContext(props);
    return (
        <ApprovalContext.Provider value={context}>
            <div style={{
                width: '100%',
                height: '100vh',
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden',
            }}>
                <Header/>
                <Body/>
            </div>
        </ApprovalContext.Provider>
    )
}

export const ApprovalLayout: React.FC<ApprovalLayoutProps> = (props) => {
    return (
        <Provider store={approvalStore}>
            <ApprovalLayoutScope {...props}/>
        </Provider>
    )
}


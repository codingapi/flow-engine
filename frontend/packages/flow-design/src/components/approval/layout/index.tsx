import React from "react";
import {ApprovalLayoutProps} from "@/components/approval/typings";
import {Provider} from "react-redux";
import {approvalStore} from "@/components/approval/store";
import {ApprovalContext} from "@/components/approval/context";
import {createApprovalContext} from "@/components/approval/hooks/use-approval-context";
import {Header} from "@/components/approval/layout/header";
import {Body} from "@/components/approval/layout/body";


const ApprovalLayoutScope: React.FC<ApprovalLayoutProps> = (props) => {
    const {context} = createApprovalContext(props);
    return (
        <ApprovalContext.Provider value={context}>
            <Header/>
            <Body/>
        </ApprovalContext.Provider>
    )
}

export const ApprovalLayout: React.FC<ApprovalLayoutProps> = (props) => {

    return (
        <Provider store={approvalStore}>
            <ApprovalLayoutScope {...props}/>
        </Provider>
    )
};


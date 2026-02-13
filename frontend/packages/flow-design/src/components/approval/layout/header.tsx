import React from "react";
import {useApprovalContext} from "@/components/approval/hooks/use-approval-context";

export const Header = () => {
    const {state,context} = useApprovalContext();

    return (
        <div>
            Header
            <button onClick={()=>{
                context.close();
            }}>close</button>
        </div>
    )
}
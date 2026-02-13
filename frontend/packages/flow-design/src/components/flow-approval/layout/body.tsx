import React from "react";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";

export const Body = () => {
    const {state,context} = useApprovalContext();

    const json = JSON.stringify(state);

    return (
        <div>
            body: {json}
        </div>
    )
}
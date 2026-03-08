import React from "react";
import {ConditionRejectViewPlugin, VIEW_KEY} from "@/components/script/plugins/action-reject-view-type";
import {ViewBindPlugin} from "@flow-engine/flow-core";
import {useDesignContext} from "@/components/design-panel/hooks/use-design-context";
import {RejectNodeManager} from "@/components/script/services/action-reject";


const useActionRejectManager = () => {
    const {state} = useDesignContext();
    const nodes = state.workflow.nodes || [];
    return new RejectNodeManager(nodes);
}


export const ConditionRejectView: React.FC<ConditionRejectViewPlugin> = (props) => {

    const rejectNodeManager = useActionRejectManager();
    const ConditionRejectViewComponent = ViewBindPlugin.getInstance().get(VIEW_KEY);

    if (ConditionRejectViewComponent) {
        return (
            <ConditionRejectViewComponent {...props} />
        );
    }

    return (
        <div
            style={{
                marginTop: "8px",
                padding: "8px",
            }}
        >
            reject
            {rejectNodeManager.getSize()}
        </div>
    )
}
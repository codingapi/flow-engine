import React from "react";
import {ConditionRejectViewPlugin, VIEW_KEY} from "@/components/script/plugins/action-reject-view-type";
import {ViewBindPlugin} from "@flow-engine/flow-core";
import {useDesignContext} from "@/components/design-panel/hooks/use-design-context";
import {ActionRejectService} from "@/components/script/services/action-reject";
import {Select} from "antd";


const useActionRejectService = (nodeId: string) => {
    const {context} = useDesignContext();
    const nodeManager = context.getPresenter().getNodeManager();
    return new ActionRejectService(nodeManager);
}

export const ConditionRejectView: React.FC<ConditionRejectViewPlugin> = (props) => {

    const rejectService = useActionRejectService(props.nodeId);
    const ConditionRejectViewComponent = ViewBindPlugin.getInstance().get(VIEW_KEY);

    if (ConditionRejectViewComponent) {
        return (
            <ConditionRejectViewComponent {...props} />
        );
    }

    const handleChange = (option: any) => {
        const script = rejectService.getScript(option);
        props.onChange?.(script);
    }

    return (
       <>
           <Select
               options={rejectService.getOptions(props.nodeId)}
               value={rejectService.getValue(props.value)}
               onChange={(value, option) => {
                   handleChange(option);
               }}
               placeholder={"请选择拒绝退回到的节点"}
           />
       </>
    )
}
import React from "react";
import {DelegateViewPlugin, VIEW_KEY} from "../delegate-type";
import {ViewBindPlugin} from "@flow-engine/flow-types";
import {Select} from "antd";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";

export const DelegateView: React.FC<DelegateViewPlugin> = (props) => {
    const DelegateViewComponent = ViewBindPlugin.getInstance().get(VIEW_KEY);

    const {state} = useApprovalContext();

    const createOperator = state.flow?.createOperator;

    const options = createOperator?[
        {
            label:createOperator.name,
            value:createOperator.id,
        }
    ]:[];

    if (DelegateViewComponent) {
        return (
            <DelegateViewComponent {...props} />
        );
    }

    const handleChange = (value: string|string[]) => {
        props.onChange?.(value);
    }

    return (
        <Select
            placeholder={"请选择委托人员"}
            mode="multiple"
            value={props.value}
            onChange={(value,option) => {
               handleChange(value);
            }}
            options={options}
        />
    )
}
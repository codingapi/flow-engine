import React from "react";
import {TransferViewPlugin, VIEW_KEY} from "../transfer-type";
import {ViewBindPlugin} from "@flow-engine/flow-core";
import {Select} from "antd";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";

export const TransferView: React.FC<TransferViewPlugin> = (props) => {
    const TransferViewComponent = ViewBindPlugin.getInstance().get(VIEW_KEY);

    const {state} = useApprovalContext();

    const createOperator = state.flow?.createOperator;

    const options = createOperator?[
        {
            label:createOperator.name,
            value:createOperator.id,
        }
    ]:[];

    if (TransferViewComponent) {
        return (
            <TransferViewComponent {...props} />
        );
    }

    const handleChange = (value: string|string[]) => {
        props.onChange?.(value);
    }

    return (
        <Select
            placeholder={"请选择转办人员"}
            mode="multiple"
            value={props.value}
            onChange={(value,option) => {
               handleChange(value);
            }}
            options={options}
        />
    )
}
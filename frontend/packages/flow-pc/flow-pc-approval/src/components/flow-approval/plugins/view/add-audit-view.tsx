import React from "react";
import {AddAuditViewPlugin, VIEW_KEY} from "../add-audit-type";
import {ViewBindPlugin} from "@flow-engine/flow-core";
import {Select} from "antd";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";

export const AddAuditView: React.FC<AddAuditViewPlugin> = (props) => {
    const AddAuditViewComponent = ViewBindPlugin.getInstance().get(VIEW_KEY);

    const {state} = useApprovalContext();

    const createOperator = state.flow?.createOperator;

    const options = createOperator?[
        {
            label:createOperator.name,
            value:createOperator.id,
        }
    ]:[];

    if (AddAuditViewComponent) {
        return (
            <AddAuditViewComponent {...props} />
        );
    }

    const handleChange = (value: string|string[]) => {
        props.onChange?.(value);
    }

    return (
        <Select
            placeholder={"请选择加签人员"}
            mode="multiple"
            value={props.value}
            onChange={(value,option) => {
               handleChange(value);
            }}
            options={options}
        />
    )
}
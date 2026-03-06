import React from "react";
import {ReturnViewPlugin, VIEW_KEY} from "../return-type";
import {ViewBindPlugin} from "@flow-engine/flow-types";
import {Select} from "antd";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";

export const ReturnView: React.FC<ReturnViewPlugin> = (props) => {
    const ReturnViewComponent = ViewBindPlugin.getInstance().get(VIEW_KEY);

    const {state} = useApprovalContext();

    const nodeOptions = React.useMemo(() => {
        const nodes = state.flow?.nodes || [];
        const currentId = state.flow?.nodeId || '';
        const options:any[] = [];
        for (const node of nodes) {
            if (currentId === node.id) {
                break;
            }
            if(node.display) {
                options.push({
                    label: node.name,
                    value: node.id,
                })
            }
        }
        return options;
    }, [state.flow?.nodes])


    if (ReturnViewComponent) {
        return (
            <ReturnViewComponent {...props} />
        );
    }

    const handleChange = (value: string | string[]) => {
        props.onChange?.(value);
    }

    return (
        <Select
            placeholder={"请选择退回节点"}
            value={props.value}
            onChange={(value, option) => {
                handleChange(value);
            }}
            options={nodeOptions}
        />
    )
}
import React from "react";
import {FlowActionProps} from "./type";
import {Button, message} from "antd";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";

export const SaveAction: React.FC<FlowActionProps> = (props) => {

    const action = props.action;
    const {context} = useApprovalContext()
    const actionPresenter = context.getPresenter().getFlowActionPresenter();

    return (
        <Button
            onClick={() => {
                actionPresenter.action(action.id).then((res) => {
                    if (res.success) {
                        message.success("流程数据已保存");
                    }
                });
            }}
        >
            {action.title}
        </Button>
    )
}
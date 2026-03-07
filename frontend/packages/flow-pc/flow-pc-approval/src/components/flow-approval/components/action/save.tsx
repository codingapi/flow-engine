import React from "react";
import {FlowActionProps} from "./type";
import {message} from "antd";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";
import {ActionButton} from "@/components/flow-approval/components/action-button";

/**
 * 保存
 * @param props
 * @constructor
 */
export const SaveAction: React.FC<FlowActionProps> = (props) => {

    const action = props.action;
    const {context} = useApprovalContext()
    const actionPresenter = context.getPresenter().getFlowActionPresenter();

    return (
        <ActionButton
            display={props.action.display}
            onClick={() => {
                actionPresenter.action(action.id).then((res) => {
                    if (res.success) {
                        message.success("流程数据已保存");
                    }
                });
            }}
            title={action.title}
        />
    )
}
import React from "react";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";
import {Button, Flex, message} from "antd";

export const Header = () => {
    const {state, context} = useApprovalContext()

    const actions = state.flow?.actions || [];

    const actionPresenter = context.getPresenter().getFlowActionPresenter();

    return (
        <Flex
            justify={"end"}
            gap={10}
        >
            {actions.map(item => {
                return (
                    <Button
                        onClick={() => {
                            const actionId = item.id;
                            actionPresenter.action(actionId).then(() => {
                                message.success("操作成功");
                                context.close();
                            });
                        }}
                    >{item.title}</Button>
                )
            })}
            <Button
                onClick={() => {
                    context.close();
                }}
            >关闭</Button>
        </Flex>
    )
}
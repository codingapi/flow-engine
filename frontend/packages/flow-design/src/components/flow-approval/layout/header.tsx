import React from "react";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";
import {Button, Flex, message} from "antd";
import {create} from "@/api/record";

export const Header = () => {
    const {state, context} = useApprovalContext()

    const actions = state.flow?.actions || [];

    const formActionContext = context.getPresenter().getFormActionContext();

    return (
        <Flex
            justify={"end"}
            gap={10}
        >
            {actions.map(item => {
                return (
                    <Button
                        onClick={()=>{
                            const actionId = item.id;
                            const formData = formActionContext.save();
                            const body = {
                                formData,
                                actionId,
                                workId: state.flow?.workId,
                            }
                            create(body).then((res)=>{
                                message.success("流程已经创建")
                            })
                        }}
                    >{item.title}</Button>
                )
            })}
            <Button onClick={() => {
                context.close();
            }}>关闭</Button>
        </Flex>
    )
}
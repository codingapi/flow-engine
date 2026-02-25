import React from "react";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";
import {message, Button, Flex, Space, Typography} from "antd";
import {ApprovalLayoutHeight} from "@/components/flow-approval/typings";

const {Title} = Typography;

export const Header = () => {
    const {state, context} = useApprovalContext()
    const actions = state.flow?.actions || [];
    const actionPresenter = context.getPresenter().getFlowActionPresenter();

    return (
        <Flex
            align="center"
            justify="space-between"
            style={{
                width: "100%",
                height: ApprovalLayoutHeight,
                padding: '0 24px',
                borderBottom: '1px solid #f0f0f0',
                backgroundColor: '#fff',
            }}
        >
            <Title level={4} style={{margin: 0, fontSize: 18}}>
                审批详情
            </Title>

            <Space size={12}>
                {actions.map((item, index) => (
                    <Button
                        key={item.id}
                        type={index === 0 ? "primary" : index === 1 ? "default" : "default"}
                        danger={index === 1}
                        onClick={() => {
                            actionPresenter.action(item.id).then(() => {
                                message.success("操作成功");
                                context.close();
                            });
                        }}
                    >
                        {item.title}
                    </Button>
                ))}
                <Button
                    type="text"
                    onClick={() => {
                        context.close();
                    }}
                >
                    关闭
                </Button>
            </Space>
        </Flex>
    )
}
import React from "react";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";
import {Button, Flex, message, Space, Typography} from "antd";
import {ApprovalLayoutHeight} from "@/components/flow-approval/typings";

const {Title} = Typography;

export const Header = () => {
    const {state, context} = useApprovalContext()
    const actions = state.flow?.actions || [];
    const actionPresenter = context.getPresenter().getFlowActionPresenter();

    return (
        <div
            style={{
                width: "100%",
                height: ApprovalLayoutHeight,
                borderBottom: '1px solid #f0f0f0',
                backgroundColor: '#fff',
            }}
        >
            <Flex
                align="center"
                justify="space-between"
                style={{
                    margin:'0 24px',
                    height: '100%',
                }}
            >
                <Title level={4} style={{fontSize: 16, fontWeight: 500}}>
                    审批详情
                </Title>

                <Space size={8}>
                    {actions.map((item, index) => (
                        <Button
                            key={item.id}
                            type={index === 0 ? "primary" : "default"}
                            danger={item.type === 'REJECT'}
                            onClick={() => {
                                actionPresenter.action(item.id).then(() => {
                                    message.success("操作成功");
                                    context.close();
                                });
                            }}
                            style={index === 0 ? {} : {}}
                        >
                            {item.title}
                        </Button>
                    ))}
                    <Button
                        onClick={() => {
                            context.close();
                        }}
                    >
                        关闭
                    </Button>
                </Space>
            </Flex>
        </div>
    )
}
import React from "react";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";
import {Button, Flex, Space, Typography} from "antd";
import {ApprovalLayoutHeight} from "@/components/flow-approval/typings";
import {ActionFactory} from "@/components/flow-approval/components/action/factory";

const {Title} = Typography;

export const Header = () => {
    const {state, context} = useApprovalContext()
    const actions = state.flow?.actions || [];

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
                    margin: '0 24px',
                    height: '100%',
                }}
            >
                <Title level={4} style={{fontSize: 16, fontWeight: 500}}>
                    {state.flow?.title || '审批详情'}
                </Title>

                <Space size={8}>
                    {actions.map((action) => {
                        return ActionFactory.getInstance().render(action);
                    })}
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
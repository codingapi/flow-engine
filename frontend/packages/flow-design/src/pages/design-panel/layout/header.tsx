import React from "react";
import {Button, Space, Tabs, message} from "antd";
import {LayoutHeaderHeight, TabPanelType} from "../types";
import {useDesignContext} from "../hooks/use-design-context";

const Left = () => {
    return (
        <div style={{
            width: 150,
        }}>流程设计面板</div>
    )
}

const Right = () => {
    const {context} = useDesignContext();

    return (
        <Space style={{
            width: 150,
        }}>
            <Button
                type="primary"
                onClick={() => {
                    context.save().then(() => {
                        message.success("流程已经保存.");
                    });
                }}
            >保存</Button>
            <Button onClick={() => {
                context.close();
            }}>关闭</Button>
        </Space>
    )
}

export const Header = () => {

    const {state, context} = useDesignContext();

    return (
        <Tabs
            style={{
                width: "100%",
                height: LayoutHeaderHeight,
            }}
            centered={true}
            items={[
                {
                    key: 'base',
                    label: '基本信息',
                },
                {
                    key: 'form',
                    label: '表单设计',
                },
                {
                    key: 'flow',
                    label: '流程设计',
                },
                {
                    key: 'setting',
                    label: '更多参数',
                },
            ]}
            defaultActiveKey={state.view.tabPanel}
            onChange={(key) => {
                context.getPresenter().updateViewPanelTab(key as TabPanelType);
            }}
            tabBarExtraContent={{
                left: <Left/>,
                right: <Right/>,
            }}
        />
    )
}
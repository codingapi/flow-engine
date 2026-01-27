import React from "react";
import {Button, Space, Tabs} from "antd";
import {LayoutHeaderHeight, PanelTabType} from "../types";
import {useContext} from "../hooks/use-context";

const Left = () => {
    return (
        <div style={{
            width: 150,
        }}>流程设计面板</div>
    )
}

const Right= () => {
    const {context} = useContext();
    return (
        <Space style={{
            width: 150,
        }}>
            <Button
                type="primary"
                onClick={() => {
                    context.save();
                }}
            >保存</Button>
            <Button onClick={() => {
                context.close();
            }}>关闭</Button>
        </Space>
    )
}

export const Header = () => {

    const {context} = useContext();

    const presenter = context.getPresenter();

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
            defaultActiveKey={context.state.panelTab}
            onChange={(key) => {
                presenter.switchPanelTab(key as PanelTabType);
            }}
            tabBarExtraContent={{
                left: <Left/>,
                right: <Right/>,
            }}
        />
    )
}
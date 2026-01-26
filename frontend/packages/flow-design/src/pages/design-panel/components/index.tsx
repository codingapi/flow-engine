import React from "react";
import {PanelTabType} from "@/pages/design-panel/types";
import {Button, Space, Tabs} from "antd";
import {useContext} from "@/pages/design-panel/hooks/use-context";

const Left = () => {
    return (
        <div style={{
            width: 150,
        }}>流程设计面板</div>
    )
}

interface RightProps {
    onClose?: () => void;
    onSave?: () => void;
}

const Right: React.FC<RightProps> = (props) => {
    return (
        <Space style={{
            width: 150,
        }}>
            <Button
                type="primary"
                onClick={() => {
                    props.onClose?.();
                }}
            >保存</Button>
            <Button onClick={() => {
                props.onClose?.();
            }}>关闭</Button>
        </Space>
    )
}

interface HeaderProps {
    onClose?: () => void;
    onSave?: () => void;
}

export const PanelHeader: React.FC<HeaderProps> = (props) => {

    const {context} = useContext();

    const presenter = context.getPresenter();

    return (
        <Tabs
            style={{
                width: "100%",
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
                right: <Right {...props} />,
            }}
        />
    )
}
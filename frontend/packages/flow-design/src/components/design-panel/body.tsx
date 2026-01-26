import React from "react";
import {Button, Space, Tabs} from "antd";
import {PanelTabType} from "@/components/design-panel/types";
import {DesignPanelContext} from "./context";
import {createContext} from "./hooks/use-context";

const Left = () => {
    return (
        <div>流程设计面板</div>
    )
}

interface RightProps {
    onClose?: () => void;
}

const Right: React.FC<RightProps> = (props) => {
    return (
        <Space>
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
    onSwitch?: (type:PanelTabType) => void;
}

const Header: React.FC<HeaderProps> = (props) => {
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
            defaultActiveKey="base"
            onChange={(key)=>{
                props.onSwitch?.(key as PanelTabType);
            }}
            tabBarExtraContent={{
                left: <Left/>,
                right: <Right onClose={props.onClose}/>,
            }}
        />
    )
}

interface PanelBodyProps {
    onClose?: () => void;
}

export const PanelBody: React.FC<PanelBodyProps> = (props) => {

    const context = createContext();

    return (
        <>
            <DesignPanelContext.Provider value={context}>
                <Header onClose={props.onClose}/>
                {context.state.value}
            </DesignPanelContext.Provider>
        </>
    )
}
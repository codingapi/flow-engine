import { Tabs,TabsProps } from "antd";
import React from "react";
import {TabBase} from "./base";
import {TabAction} from "./action";
import {TabPromission} from "./promission";

interface NodeTapsProps {

}

export const NodeTaps:React.FC<NodeTapsProps> = (props) => {

    const items:TabsProps['items'] = [
        {
            key: 'base',
            label: `基础配置`,
            children: <TabBase/>
        },
        {
            key: 'action',
            label: `按钮配置`,
            children: <TabAction/>,
        },
        {
            key: 'promission',
            label: `权限配置`,
            children: <TabPromission/>,
        }
    ];

    return (
        <Tabs
            style={{
                width: '100%',
            }}
            centered={true}
            items={items}
        />
    )
}
import {Tabs, TabsProps} from "antd";
import React from "react";
import {TabBase} from "./base";
import {TabAction} from "./action";
import {TabPromission} from "./promission";
import {useNodeDisplayManager} from "@/components/editor/hooks";


interface NodeTapsProps {

}

export const NodeTaps: React.FC<NodeTapsProps> = (props) => {
    const nodeDisplayManager = useNodeDisplayManager();

    const items: TabsProps['items'] = [];

    items.push({
        key: 'base',
        label: `基础配置`,
        children: <TabBase/>,
        destroyOnHidden: true,
    });

    if (nodeDisplayManager.showAction()) {
        items.push({
            key: 'action',
            label: `按钮配置`,
            children: <TabAction/>,
            destroyOnHidden: true,
        })
    }
    if (nodeDisplayManager.showPromission()) {
        items.push({
            key: 'promission',
            label: `权限配置`,
            children: <TabPromission/>,
            destroyOnHidden: true,
        });
    }

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
import {Tabs, TabsProps} from "antd";
import React, {useContext} from "react";
import {TabBase} from "./base";
import {TabAction} from "./action";
import {TabPromission} from "./promission";
import {NodeRenderContext} from "@/components/editor/context";
import {ActionManager} from "@/components/editor/node-components/action";
import {StrategyManager} from "@/components/editor/node-components/strategy";

interface NodeTapsProps {

}

export const NodeTaps: React.FC<NodeTapsProps> = (props) => {
    const {node} = useContext(NodeRenderContext);
    const actions = node.getNodeRegistry()?.meta.actions || [];
    const strategies = node.getNodeRegistry()?.meta.strategies || [];
    const actionManager = React.useCallback(() => {
        return new ActionManager(actions);
    }, [actions]);

    const strategyManager = React.useCallback(() => {
        return new StrategyManager(strategies);
    }, [strategies]);

    const items: TabsProps['items'] = [];

    items.push({
        key: 'base',
        label: `基础配置`,
        children: <TabBase/>,
        destroyOnHidden: true,
    });

    if (!actionManager().isEmpty()) {
        items.push({
            key: 'action',
            label: `按钮配置`,
            children: <TabAction/>,
            destroyOnHidden: true,
        })
    }

    if (strategyManager().getComponent('FormFieldPermissionStrategy')) {
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
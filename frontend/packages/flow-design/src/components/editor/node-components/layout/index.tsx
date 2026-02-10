import React from "react";
import {TabBase} from "@/components/editor/node-components/taps/base";
import {TabAction} from "@/components/editor/node-components/taps/action";
import {TabPromission} from "@/components/editor/node-components/taps/promission";
import { TabsProps,Tabs } from "antd";

interface TabNodeLayoutProps{
    children?:React.ReactNode;
    hiddenAction?:boolean;
}

export const TabNodeLayout:React.FC<TabNodeLayoutProps> = (props)=>{

    const items: TabsProps['items'] = [];

    const hiddenAction = props.hiddenAction || false;

    items.push({
        key: 'base',
        label: `基础配置`,
        children: <TabBase children={props.children}/>,
        destroyOnHidden: true,
    });

    if(!hiddenAction) {
        items.push({
            key: 'action',
            label: `按钮配置`,
            children: <TabAction/>,
            destroyOnHidden: true,
        })
    }

    items.push({
        key: 'promission',
        label: `权限配置`,
        children: <TabPromission/>,
        destroyOnHidden: true,
    });

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

interface PanelLayoutProps{
    children?:React.ReactNode;
}

export const PanelLayout:React.FC<PanelLayoutProps> = (props)=>{
    return (
        <TabBase children={props.children}/>
    )
}
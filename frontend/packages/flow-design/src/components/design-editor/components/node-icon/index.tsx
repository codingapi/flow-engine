import React from "react";
import {NodeType} from "@/components/design-editor/typings/node-type";
import {Button, Flex, Space,Input,theme} from "antd";
import {ApiOutlined, AuditOutlined, BellOutlined, BranchesOutlined, ClockCircleOutlined, CloseOutlined, EditOutlined,
    MergeOutlined,
    NodeExpandOutlined, PoweroffOutlined, PullRequestOutlined, ShareAltOutlined, UserOutlined} from "@ant-design/icons";


interface NodeIconProps {
    type: NodeType;
}

export const NodeIcon: React.FC<NodeIconProps> = (props) => {
    const icon = props.type as NodeType;
    const {token} = theme.useToken();
    const style = {
        fontSize: 18,
        marginRight: 4,
        marginLeft: 4,
        color: token.colorPrimary
    }
    if (!icon) return null;
    if (icon === 'APPROVAL') {
        return <AuditOutlined style={style}/>
    }
    if (icon === 'CONDITION') {
        return <BranchesOutlined style={style}/>
    }
    if (icon === 'CONDITION_BRANCH') {
        return <BranchesOutlined style={style}/>
    }
    if (icon === 'DELAY') {
        return <ClockCircleOutlined style={style}/>
    }
    if (icon === 'END') {
        return <PoweroffOutlined style={style}/>
    }
    if (icon === 'HANDLE') {
        return <EditOutlined style={style}/>
    }
    if (icon === 'INCLUSIVE') {
        return <MergeOutlined style={style}/>
    }
    if (icon === 'INCLUSIVE_BRANCH') {
        return <MergeOutlined style={style}/>
    }
    if (icon === 'NOTIFY') {
        return <BellOutlined style={style}/>
    }
    if (icon === 'PARALLEL') {
        return <NodeExpandOutlined style={style}/>
    }
    if (icon === 'PARALLEL_BRANCH') {
        return <NodeExpandOutlined style={style}/>
    }
    if (icon === 'ROUTER') {
        return <PullRequestOutlined style={style}/>
    }
    if (icon === 'START') {
        return <UserOutlined style={style}/>
    }
    if (icon === 'SUB_PROCESS') {
        return <ShareAltOutlined style={style}/>
    }
    if (icon === 'TRIGGER') {
        return <ApiOutlined style={style}/>
    }
    return null;

}
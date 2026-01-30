import React, {useContext} from "react";
import {useIsSidebar} from "@/components/editor/hooks";
import {Button, Flex, Space,Input,theme} from "antd";
import {nodeFormPanelFactory} from "@/components/editor/components/sidebar";
import {usePanelManager} from "@flowgram.ai/panel-manager-plugin";
import {NodeRenderContext} from "@/components/editor/context";
import {Field, FieldRenderProps, FlowNodeEntity} from "@flowgram.ai/fixed-layout-editor";

import {FlowNodeRegistry} from '../../typings';
import {ApiOutlined, AuditOutlined, BellOutlined, BranchesOutlined, ClockCircleOutlined, CloseOutlined, EditOutlined,
    MergeOutlined,
    NodeExpandOutlined, PoweroffOutlined, PullRequestOutlined, ShareAltOutlined, UserOutlined} from "@ant-design/icons";
import {NodeType} from "@/components/editor/typings/node-type";



const getIcon = (node: FlowNodeEntity) => {
    const icon = node.getNodeRegistry<FlowNodeRegistry>().info?.icon as NodeType;
    const {token} = theme.useToken();
    const style = {
        fontSize: 18,
        marginRight: 4,
        marginLeft:4,
        color:token.colorPrimary
    }
    if (!icon) return null;
    if(icon ==='APPROVAL'){
        return <AuditOutlined style={style}/>
    }
    if(icon ==='CONDITION'){
        return <BranchesOutlined  style={style}/>
    }
    if(icon ==='CONDITION_BRANCH'){
        return <BranchesOutlined  style={style}/>
    }
    if(icon ==='DELAY'){
        return <ClockCircleOutlined  style={style}/>
    }
    if(icon ==='END'){
        return <PoweroffOutlined  style={style}/>
    }
    if(icon ==='HANDLE'){
        return <EditOutlined  style={style}/>
    }
    if(icon ==='INCLUSIVE'){
        return <MergeOutlined  style={style}/>
    }
    if(icon ==='INCLUSIVE_BRANCH'){
        return <MergeOutlined style={style}/>
    }
    if(icon ==='NOTIFY'){
        return <BellOutlined  style={style}/>
    }
    if(icon ==='PARALLEL'){
        return <NodeExpandOutlined  style={style}/>
    }
    if(icon ==='PARALLEL_BRANCH'){
        return <NodeExpandOutlined style={style}/>
    }
    if(icon ==='ROUTER'){
        return <PullRequestOutlined  style={style}/>
    }
    if(icon ==='START'){
        return <UserOutlined style={style}/>
    }
    if(icon ==='SUB_PROCESS'){
        return <ShareAltOutlined  style={style}/>
    }
    if(icon ==='TRIGGER'){
        return <ApiOutlined  style={style}/>
    }
    return null;
};

interface HeaderTitleProps{
    title:string;
    onChange:(value:string)=>void;
}

const HeaderTitle:React.FC<HeaderTitleProps> = (props)=>{

    const {readonly} = useContext(NodeRenderContext);
    const isSidebar = useIsSidebar();
    const [editTitle, setEditTitle] = React.useState(false);

    if(readonly || !isSidebar){
        return <span>{props.title}</span>
    }

    if(editTitle){
        return (
            <Input
                autoFocus
                size={"small"}
                style={{width: 200}}
                value={props.title}
                onChange={(e)=>{
                    props.onChange(e.target.value);
                }}
                onBlur={()=>{
                    setEditTitle(false);
                }}
                onPressEnter={()=>{
                    setEditTitle(false);
                }}
            />
        )
    }

    return (
        <Space>
            <span>{props.title}</span>
            <EditOutlined onClick={()=>{
                 setEditTitle(true);
            }}/>
        </Space>
    )
}


export const NodeHeader = () => {
    const {node} = useContext(NodeRenderContext);
    const isSidebar = useIsSidebar();
    const panelManager = usePanelManager();

    const handleClose = () => {
        panelManager.close(nodeFormPanelFactory.key);
    };
    const HeaderIcon = getIcon(node);
    return (
        <Flex
            style={{
                width: "100%",
                padding: "4px 5px",
                borderBottom: "1px solid #f0f0f0",
                marginBottom: 8,
            }}
            vertical={false}
            justify={"space-between"}
            align={"center"}
        >
            <Space>
                {HeaderIcon}
                <Field name={"title"}>
                    {({field: {value, onChange}, fieldState}: FieldRenderProps<string>) => (
                       <HeaderTitle title={value} onChange={(e)=>{onChange(e)}}/>
                    )}
                </Field>
            </Space>

            {isSidebar && (
                <Button
                    type={"text"}
                    icon={<CloseOutlined />}
                    onClick={() => {
                        handleClose();
                    }}/>
            )}
        </Flex>
    )
}
import React, {useContext} from "react";
import {useIsSidebar} from "@/components/editor/hooks";
import {Button, Flex, Input, Space} from "antd";
import {nodeFormPanelFactory} from "@/components/editor/components/sidebar";
import {usePanelManager} from "@flowgram.ai/panel-manager-plugin";
import {NodeRenderContext} from "@/components/editor/context";
import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";
import {CloseOutlined, EditOutlined} from "@ant-design/icons";
import {NodeIcon} from "@/components/editor/components/node-icon";
import {NodeType} from "@/components/editor/typings/node-type";
import {FlowNodeRegistry} from "@/components/editor/typings";


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
    const nodeType  = node.getNodeRegistry<FlowNodeRegistry>().type as NodeType;

    const handleClose = () => {
        panelManager.close(nodeFormPanelFactory.key);
    };
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
                <NodeIcon type={nodeType}/>
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
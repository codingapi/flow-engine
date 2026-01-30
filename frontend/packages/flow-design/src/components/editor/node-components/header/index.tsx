import React, {useCallback, useContext, useMemo, useState} from "react";
import {useIsSidebar} from "@/components/editor/hooks";
import {Button, Flex, Input, Space, theme} from "antd";
import {nodeFormPanelFactory} from "@/components/editor/components/sidebar";
import {usePanelManager} from "@flowgram.ai/panel-manager-plugin";
import {NodeRenderContext} from "@/components/editor/context";
import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";
import {CloseOutlined, EditOutlined} from "@ant-design/icons";
import {NodeIcon} from "@/components/editor/components/node-icon";
import {NodeType} from "@/components/editor/typings/node-type";
import {FlowNodeRegistry} from "@/components/editor/typings";

interface HeaderTitleProps {
    title: string;
    onChange: (value: string) => void;
    readonly?: boolean;
}

const HeaderTitle: React.FC<HeaderTitleProps> = ({ title, onChange, readonly }) => {
    const { token } = theme.useToken();
    const [editTitle, setEditTitle] = useState(false);
    const isSidebar = useIsSidebar();
    const { node } = useContext(NodeRenderContext);

    const registry = node.getNodeRegistry<FlowNodeRegistry>();
    const editTitleDisabled = useMemo(() => {
        const { meta } = registry;
        return meta?.editTitleDisable ?? false;
    }, [registry, node]);

    const handleChange = useCallback((value: string) => {
        const trimmed = value.trim();
        if (trimmed) {
            onChange(trimmed);
        }
    }, [onChange]);

    if (readonly || !isSidebar || editTitleDisabled) {
        return <span>{title}</span>;
    }

    if (editTitle) {
        return (
            <Input
                autoFocus
                size="small"
                style={{ width: 200 }}
                defaultValue={title}
                onChange={(e)=>{
                    handleChange(e.target.value);
                }}
                onBlur={() => setEditTitle(false)}
                onPressEnter={(e) => {
                    handleChange((e.target as HTMLInputElement).value);
                    setEditTitle(false);
                }}
            />
        );
    }

    return (
        <Space>
            <span>{title}</span>
            <EditOutlined
                style={{ color: token.colorPrimary, cursor: "pointer" }}
                onClick={() => setEditTitle(true)}
            />
        </Space>
    );
};

interface NodeHeaderProps{
    style?: React.CSSProperties;
    iconEnable?: boolean;
}

export const NodeHeader: React.FC<NodeHeaderProps> = (props) => {
    const { node } = useContext(NodeRenderContext);
    const isSidebar = useIsSidebar();
    const panelManager = usePanelManager();
    const { token } = theme.useToken();
    const iconEnable = props.iconEnable ?? true;

    const nodeType = node.getNodeRegistry<FlowNodeRegistry>().type as NodeType;

    const handleClose = useCallback(() => {
        panelManager.close(nodeFormPanelFactory.key);
    }, [panelManager]);

    const headerStyle: React.CSSProperties = {
        width: "100%",
        padding: isSidebar ? "4px 5px" : "5px",
        borderBottom: "1px solid #f0f0f0",
        marginBottom: 8,
        position: "relative",
        ...props.style,
    };

    return (
        <Flex
            style={headerStyle}
            justify="space-between"
            align="center"
        >
            <Space>
                {iconEnable && <NodeIcon type={nodeType} />}
                <Field name="title">
                    {({ field: { value, onChange } }: FieldRenderProps<string>) => (
                        <HeaderTitle
                            title={value}
                            onChange={onChange}
                        />
                    )}
                </Field>
            </Space>

            {isSidebar && (
                <Button
                    type="text"
                    icon={<CloseOutlined style={{ color: token.colorPrimary }} />}
                    onClick={handleClose}
                />
            )}
        </Flex>
    );
};
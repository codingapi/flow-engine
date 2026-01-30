import React, { useCallback, useContext, useMemo, useState } from "react";
import { useIsSidebar } from "@/components/editor/hooks";
import { Button, Flex, Input, Space, theme } from "antd";
import { nodeFormPanelFactory } from "@/components/editor/components/sidebar";
import { usePanelManager } from "@flowgram.ai/panel-manager-plugin";
import { NodeRenderContext } from "@/components/editor/context";
import { Field, FieldRenderProps, useClientContext } from "@flowgram.ai/fixed-layout-editor";
import { CloseCircleOutlined, CloseOutlined, EditOutlined } from "@ant-design/icons";
import { NodeIcon } from "@/components/editor/components/node-icon";
import { NodeType } from "@/components/editor/typings/node-type";
import { FlowNodeRegistry } from "@/components/editor/typings";

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

export const NodeHeader: React.FC = () => {
    const { node, deleteNode } = useContext(NodeRenderContext);
    const isSidebar = useIsSidebar();
    const panelManager = usePanelManager();
    const clientContext = useClientContext();
    const { token } = theme.useToken();
    const [isHovered, setIsHovered] = useState(false);

    const nodeType = node.getNodeRegistry<FlowNodeRegistry>().type as NodeType;
    const registry = node.getNodeRegistry<FlowNodeRegistry>();

    const handleClose = useCallback(() => {
        panelManager.close(nodeFormPanelFactory.key);
    }, [panelManager]);

    const deleteDisabled = useMemo(() => {
        const { canDelete, meta } = registry;

        if (typeof canDelete === 'function') {
            return !canDelete(clientContext, node);
        }

        return meta?.deleteDisable ?? false;
    }, [registry, clientContext, node]);

    const handleDelete = useCallback((e: React.MouseEvent) => {
        e.stopPropagation();
        deleteNode();
    }, [deleteNode]);

    const headerStyle: React.CSSProperties = {
        width: "100%",
        padding: isSidebar ? "4px 5px" : "5px",
        borderBottom: "1px solid #f0f0f0",
        marginBottom: 8,
        position: "relative",
    };

    // 或者使用 marginTop 方案（根据原代码）
    const deleteButtonStyleV2: React.CSSProperties = {
        marginRight: -10,
        marginTop: -42,
        cursor: "pointer",
        opacity: isHovered ? 1 : 0,
        visibility: isHovered ? "visible" : "hidden",
        transition: "opacity 0.2s ease, visibility 0.2s ease",
        zIndex: 10,
    };

    return (
        <Flex
            style={headerStyle}
            justify="space-between"
            align="center"
            onMouseEnter={() => setIsHovered(true)}
            onMouseLeave={() => setIsHovered(false)}
        >
            <Space>
                <NodeIcon type={nodeType} />
                <Field name="title">
                    {({ field: { value, onChange } }: FieldRenderProps<string>) => (
                        <HeaderTitle
                            title={value}
                            onChange={onChange}
                        />
                    )}
                </Field>
            </Space>

            {isSidebar ? (
                <Button
                    type="text"
                    icon={<CloseOutlined style={{ color: token.colorPrimary }} />}
                    onClick={handleClose}
                />
            ) : !deleteDisabled && (
                <div style={deleteButtonStyleV2}>
                    <CloseCircleOutlined
                        style={{
                            color: token.colorPrimary,
                            transition: "transform 0.2s ease",
                            transform: isHovered ? "scale(1.1)" : "scale(1)",
                        }}
                        onClick={handleDelete}
                    />
                </div>
            )}
        </Flex>
    );
};
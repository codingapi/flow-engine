import React, {useCallback, useContext, useMemo, useState} from "react";
import {Flex, theme} from "antd";
import {CloseCircleOutlined} from "@ant-design/icons";
import {NodeRenderContext} from "@/components/editor/context";
import {FlowNodeRegistry} from "@/components/editor/typings";
import {useClientContext} from "@flowgram.ai/fixed-layout-editor";

interface NodePanelProps {
    children?: React.ReactNode;
}

export const NodePanel: React.FC<NodePanelProps> = (props) => {
    const [isHovered, setIsHovered] = useState(false);
    const {node, deleteNode} = useContext(NodeRenderContext);
    const clientContext = useClientContext();
    const registry = node.getNodeRegistry<FlowNodeRegistry>();

    const {token} = theme.useToken();
    // 或者使用 marginTop 方案（根据原代码）
    const deleteButtonStyleV2: React.CSSProperties = {
        right: -10,
        top: -12,
        cursor: "pointer",
        opacity: isHovered ? 1 : 0,
        visibility: isHovered ? "visible" : "hidden",
        transition: "opacity 0.2s ease, visibility 0.2s ease",
        zIndex: 10,
        position: "absolute",
    };

    const deleteDisabled = useMemo(() => {
        const {canDelete, meta} = registry;

        if (typeof canDelete === 'function') {
            return !canDelete(clientContext, node);
        }

        return meta?.deleteDisable ?? false;
    }, [registry, clientContext, node]);

    const handleDelete = useCallback((e: React.MouseEvent) => {
        e.stopPropagation();
        deleteNode();
    }, [deleteNode]);

    return (
        <div
            style={{
                width: '100%',
                padding: 3
            }}
            onMouseEnter={() => setIsHovered(true)}
            onMouseLeave={() => setIsHovered(false)}
        >
            {!deleteDisabled && (
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
            <Flex
                vertical={true}
                justify="center"
                align="center"
            >
                {props.children}
            </Flex>
        </div>
    )
}
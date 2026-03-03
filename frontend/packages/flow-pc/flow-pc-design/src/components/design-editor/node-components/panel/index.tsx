import React, {useCallback, useContext, useMemo, useState} from "react";
import {Flex, theme} from "antd";
import {CloseCircleOutlined} from "@ant-design/icons";
import {NodeFormContext, NodeRenderContext} from "@/components/design-editor/context";
import {FlowNodeJSON, FlowNodeRegistry} from "@/components/design-editor/typings";
import {FormRenderProps, useClientContext} from "@flowgram.ai/fixed-layout-editor";
import {useIsSidebar} from "@/components/design-editor/hooks";

interface NodePanelProps {
    children?: React.ReactNode;
    data: FormRenderProps<FlowNodeJSON['data']>;
}

export const NodePanel: React.FC<NodePanelProps> = (props) => {
    return (
        <NodeFormContext.Provider value={props.data}>
            <$NodePanel
                {...props}
            />
        </NodeFormContext.Provider>
    )
}


export const $NodePanel: React.FC<NodePanelProps> = (props) => {
    const [isHovered, setIsHovered] = useState(false);
    const {node, deleteNode} = useContext(NodeRenderContext);
    const clientContext = useClientContext();
    const registry = node.getNodeRegistry<FlowNodeRegistry>();
    const isSidebar = useIsSidebar();
    const ctx = useClientContext();
    const {playground} = ctx;
    const {token} = theme.useToken();

    const canDeleteNode = playground.config.readonlyOrDisabled;

    const deleteButtonStyle: React.CSSProperties = {
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

    if (isSidebar || canDeleteNode) {
        return (
            <div
                style={{
                    width: '100%',
                    padding: 3
                }}
            >
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
                <div style={deleteButtonStyle}>
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
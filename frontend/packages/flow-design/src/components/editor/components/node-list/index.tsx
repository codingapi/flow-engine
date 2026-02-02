import {FlowNodeEntity, useClientContext} from "@flowgram.ai/fixed-layout-editor";
import React from "react";
import {FlowNodeRegistry} from "@/components/editor/typings";
import styled from 'styled-components';
import {FlowNodeRegistries} from "@/components/editor/nodes";
import {NodeIcon} from "@/components/editor/components/node-icon";
import {NodeType} from "@/components/editor/typings/node-type";
import {useDesignContext} from "@/pages/design-panel/hooks/use-design-context";

const NodesWrap = styled.div`
    max-height: 500px;
    overflow: auto;

    &::-webkit-scrollbar {
        display: none;
    }
`;

const NodeWrap = styled.div`
    width: 100%;
    height: 32px;
    border-radius: 5px;
    display: flex;
    align-items: center;
    cursor: pointer;
    font-size: 19px;
    padding: 0 15px;

    &:hover {
        background-color: hsl(252deg 62% 55% / 9%);
        color: hsl(252 62% 54.9%);
    }

,
`;

const NodeLabel = styled.div`
    font-size: 12px;
    margin-left: 10px;
`;

interface NodeListProps {
    onSelect: (meta: any) => void;
    from: FlowNodeEntity;
}

function Node(props: { label: string; icon: JSX.Element; onClick: () => void; disabled: boolean }) {
    return (
        <NodeWrap
            onClick={props.disabled ? undefined : props.onClick}
            style={props.disabled ? {opacity: 0.3} : {}}
        >
            <div style={{fontSize: 14}}>{props.icon}</div>
            <NodeLabel>{props.label}</NodeLabel>
        </NodeWrap>
    );
}

export const NodeList: React.FC<NodeListProps> = (props) => {
    const clientContext = useClientContext();
    const {context} = useDesignContext();
    const presenter = context.getPresenter();
    const handleClick = (registry: FlowNodeRegistry) => {
        const nodeType = registry.type;
        presenter.createNode(nodeType as string).then(currentNode => {
            props.onSelect?.(currentNode);
        });
    };

    return (
        <NodesWrap style={{width: 80 * 2 + 20}}>
            {FlowNodeRegistries.filter((registry) => !registry.meta?.addDisable).map((registry) => (
                <Node
                    key={registry.type}
                    disabled={!(registry.canAdd?.(clientContext, props.from) ?? true)}
                    icon={<NodeIcon type={registry.type as NodeType}/>}
                    label={registry.info.description as string}
                    onClick={() => handleClick(registry)}
                />
            ))}
        </NodesWrap>
    )
}
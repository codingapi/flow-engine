import {type FlowNodeEntity, useClientContext} from '@flowgram.ai/fixed-layout-editor';
import {Container} from './styles';
import {PlusOutlined} from "@ant-design/icons";
import React from "react";
import {ConditionBranchNodeRegistry} from '../../nodes/condition-branch';
import {PARALLELBranchNodeRegistry} from "@/components/editor/nodes/parallel-branch";
import {InclusiveBranchNodeRegistry} from "@/components/editor/nodes/inclusive-branch";

interface BranchAdderPropsType {
    activated?: boolean;
    node: FlowNodeEntity;
}

export const BranchAdder: React.FC<BranchAdderPropsType> = (props: BranchAdderPropsType) => {
    const {activated, node} = props;
    const nodeData = node.firstChild!.renderData;
    const ctx = useClientContext();
    const {operation, playground} = ctx;
    const {isVertical} = node;

    function addBranch() {
        const nodeType = node.flowNodeType;
        let block: FlowNodeEntity;
        if (nodeType === 'CONDITION') {
            block = operation.addBlock(
                node,
                ConditionBranchNodeRegistry.onAdd!(ctx, node),
                {
                    index: 0,
                }
            )
        }
        if (nodeType === 'PARALLEL') {
            block = operation.addBlock(
                node,
                PARALLELBranchNodeRegistry.onAdd!(ctx, node),
                {
                    index: 0,
                }
            )
        }
        if (nodeType === 'INCLUSIVE') {
            block = operation.addBlock(
                node,
                InclusiveBranchNodeRegistry.onAdd!(ctx, node),
                {
                    index: 0,
                }
            )
        }
        setTimeout(() => {
            playground.scrollToView({
                bounds: block.bounds,
                scrollToCenter: true,
            });
        }, 10);
    }

    if (playground.config.readonlyOrDisabled) return null;

    return (
        <Container
            isVertical={isVertical}
            activated={activated}
            onMouseEnter={() => nodeData?.toggleMouseEnter()}
            onMouseLeave={() => nodeData?.toggleMouseLeave()}
        >
            <div
                onClick={() => {
                    addBranch();
                }}
                aria-hidden="true"
                style={{flexGrow: 1, textAlign: 'center'}}
            >
                <PlusOutlined/>
            </div>
        </Container>
    );
}

import {type FlowNodeEntity, useClientContext} from '@flowgram.ai/fixed-layout-editor';
import React, {useCallback, useContext} from "react";
import {NodeRenderContext} from "@/components/design-editor/context";
import {NodePanel} from "@/components/design-editor/node-components/panel";
import {NodeHeader} from "@/components/design-editor/node-components/header";
import {Button} from "antd";
import {nodeFormPanelFactory} from "@/components/design-editor/components/sidebar";
import {usePanelManager} from "@flowgram.ai/panel-manager-plugin";
import {useDesignContext} from "@/components/design-panel/hooks/use-design-context";
import {NodeType} from "@/components/design-editor/typings/node-type";

interface BranchAdderPropsType {
    activated?: boolean;
    node: FlowNodeEntity;
}

export const BranchAdder: React.FC<BranchAdderPropsType> = (props: BranchAdderPropsType) => {
    return null;
}

interface BranchAdderProps {
    buttonText: string;
    addType: NodeType
}

export const BranchAdderRender: React.FC<BranchAdderProps> = (props) => {

    const {node} = useContext(NodeRenderContext);
    const ctx = useClientContext();
    const {operation, playground} = ctx;
    const panelManager = usePanelManager();
    const {context} = useDesignContext();
    const presenter = context.getPresenter();

    const handleAddBranch = () => {
        presenter.createNode(node.id, props.addType).then(block => {
            operation.addBlock(
                node,
                block,
                {
                    index: 0,
                }
            );
            setTimeout(() => {
                handleClose();
            }, 10)
        });
    }

    const handleClose = useCallback(() => {
        panelManager.close(nodeFormPanelFactory.key);
    }, [panelManager]);

    const canAddBranch = playground.config.readonlyOrDisabled;

    return (
        <NodePanel>
            <NodeHeader
                iconEnable={true}
                style={{
                    width: 120
                }}/>
            <Button type={'link'} disabled={canAddBranch} onClick={handleAddBranch}>{props.buttonText}</Button>
        </NodePanel>
    );
};

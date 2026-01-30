import {FixedLayoutPluginContext, type FlowNodeEntity, useClientContext} from '@flowgram.ai/fixed-layout-editor';
import React, {useCallback, useContext} from "react";
import {FlowNodeJSON} from "@/components/editor/typings";
import {NodeRenderContext} from "@/components/editor/context";
import {NodePanel} from "@/components/editor/node-components/panel";
import {NodeHeader} from "@/components/editor/node-components/header";
import {Button} from "antd";
import {nodeFormPanelFactory} from "@/components/editor/components/sidebar";
import { usePanelManager } from "@flowgram.ai/panel-manager-plugin";

interface BranchAdderPropsType {
    activated?: boolean;
    node: FlowNodeEntity;
}

export const BranchAdder: React.FC<BranchAdderPropsType> = (props: BranchAdderPropsType) => {
    return null;
}

interface BranchAdderProps {
    buttonText: string;
    onAdd?: (ctx: FixedLayoutPluginContext, from: FlowNodeEntity) => FlowNodeJSON;
}

export const BranchAdderRender:React.FC<BranchAdderProps> = (props) => {

    const {node} = useContext(NodeRenderContext);
    const ctx = useClientContext();
    const {operation, playground} = ctx;
    const panelManager = usePanelManager();

    const handleAddBranch = () => {
        operation.addBlock(
            node,
            props.onAdd!(ctx, node),
            {
                index: 0,
            }
        );

        setTimeout(()=>{
            handleClose();
        },10)
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

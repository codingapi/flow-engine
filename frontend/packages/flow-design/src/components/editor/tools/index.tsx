import React from "react";
import { ToolContainer, ToolSection } from '../styles';
import {SwitchVertical} from "@/components/editor/tools/switch-vertical";
import {ZoomSelect} from "@/components/editor/tools/zoom-select";
import { usePlaygroundTools } from "@flowgram.ai/fixed-layout-editor";
import {FitView} from "@/components/editor/tools/fit-view";
export const EditorTools = () => {

    const tools = usePlaygroundTools();

    return (
        <ToolContainer >
            <ToolSection>
                <SwitchVertical/>
                <ZoomSelect />
                <FitView fitView={tools.fitView} />
            </ToolSection>
        </ToolContainer>
    )
}
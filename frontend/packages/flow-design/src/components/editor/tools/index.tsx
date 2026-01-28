import React from "react";
import { ToolContainer, ToolSection } from '../styles';
import {SwitchVertical} from "@/components/editor/tools/switch-vertical";
import {ZoomSelect} from "@/components/editor/tools/zoom-select";
import { usePlaygroundTools } from "@flowgram.ai/fixed-layout-editor";
import {FitView} from "@/components/editor/tools/fit-view";
import {MinimapSwitch} from "@/components/editor/tools/minimap-switch";
import {Minimap} from "@/components/editor/tools/minimap";
import {Readonly} from "@/components/editor/tools/readonly";
import {Undo} from "@/components/editor/tools/undo";
import {Redo} from "@/components/editor/tools/redo";
import {DownloadTool} from "@/components/editor/tools/download";
export const EditorTools = () => {

    const tools = usePlaygroundTools();
    const [minimapVisible, setMinimapVisible] = React.useState(false);
    return (
        <ToolContainer >
            <ToolSection>
                <SwitchVertical/>
                <ZoomSelect />
                <FitView fitView={tools.fitView} />
                <MinimapSwitch minimapVisible={minimapVisible} setMinimapVisible={setMinimapVisible} />
                <Minimap visible={minimapVisible} />
                <Readonly />
                <Undo/>
                <Redo/>
                <DownloadTool/>

            </ToolSection>
        </ToolContainer>
    )
}
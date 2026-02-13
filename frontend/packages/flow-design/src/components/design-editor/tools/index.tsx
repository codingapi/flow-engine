import React from "react";
import {ToolContainer, ToolSection} from '../styles';
import {SwitchVertical} from "@/components/design-editor/tools/switch-vertical";
import {ZoomSelect} from "@/components/design-editor/tools/zoom-select";
import {usePlaygroundTools} from "@flowgram.ai/fixed-layout-editor";
import {FitView} from "@/components/design-editor/tools/fit-view";
import {MinimapSwitch} from "@/components/design-editor/tools/minimap-switch";
import {Minimap} from "@/components/design-editor/tools/minimap";
import {Readonly} from "@/components/design-editor/tools/readonly";
import {Undo} from "@/components/design-editor/tools/undo";
import {Redo} from "@/components/design-editor/tools/redo";
import {DownloadTool} from "@/components/design-editor/tools/download";
import {Interactive} from "@/components/design-editor/tools/interactive";

export const EditorTools = () => {

    const tools = usePlaygroundTools();
    const [minimapVisible, setMinimapVisible] = React.useState(false);
    return (
        <ToolContainer >
            <ToolSection>
                <Interactive/>
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
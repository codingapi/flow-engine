import React from "react";
import {DesignPanelContext} from "./context";
import {createContext} from "./hooks/use-context";
import {PanelHeader} from "@/components/header";
import {Panel} from "@/components/panel";


interface PanelBodyProps {
    onClose?: () => void;
}

export const PanelBody: React.FC<PanelBodyProps> = (props) => {

    const context = createContext();

    return (
        <>
            <DesignPanelContext.Provider value={context}>
                <PanelHeader onClose={props.onClose}/>
                <Panel>
                    {context.state.value} {context.state.panelTab}
                </Panel>
            </DesignPanelContext.Provider>
        </>
    )
}
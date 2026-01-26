import React from "react";
import {DesignPanelContext} from "../context";
import {createContext} from "../hooks/use-context";
import {PanelHeader} from "@/pages/design-panel/components/index";
import {Panel} from "@/components/panel";
import {Provider} from "react-redux";
import {designStore} from "@/pages/design-panel/store";


interface PanelBodyProps {
    onClose?: () => void;
}


const PanelBodyScope: React.FC<PanelBodyProps> = (props) => {
    const {state,context} = createContext();

    return (
        <DesignPanelContext.Provider value={context}>
            <PanelHeader onClose={props.onClose}/>
            <Panel>
                {state.value} {state.panelTab}
            </Panel>
        </DesignPanelContext.Provider>
    )
}

export const PanelBody: React.FC<PanelBodyProps> = (props) => {
    return (
        <>
            <Provider store={designStore}>
                <PanelBodyScope {...props}/>
            </Provider>
        </>
    )
}
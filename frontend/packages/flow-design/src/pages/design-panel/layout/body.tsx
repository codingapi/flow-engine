import React from "react";
import {useContext} from "../hooks/use-context";
import {TabForm} from "@/pages/design-panel/tabs/form";
import {TabSetting} from "@/pages/design-panel/tabs/setting";
import {TabBase} from "@/pages/design-panel/tabs/base";
import {TabFlow} from "@/pages/design-panel/tabs/flow";

export const Body = ()=>{
    const {state} = useContext();
    const tabPanelType = state.panelTab;
    return (
        <>
            {tabPanelType ==='form' && (
                <TabForm/>
            )}
            {tabPanelType ==='flow' && (
                <TabFlow/>
            )}
            {tabPanelType ==='base' && (
                <TabBase/>
            )}
            {tabPanelType ==='setting' && (
                <TabSetting/>
            )}
        </>
    )
}



import React from "react";
import {useDesignContext} from "../hooks/use-design-context";
import {TabForm} from "@/components/design-panel/tabs/form";
import {TabSetting} from "@/components/design-panel/tabs/setting";
import {TabBase} from "@/components/design-panel/tabs/base";
import {TabFlow} from "@/components/design-panel/tabs/flow";

export const Body = ()=>{
    const {state} = useDesignContext();
    const tabPanelType = state.view.tabPanel;

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



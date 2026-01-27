import React from "react";
import {Panel} from "@/components/panel";
import {useContext} from "../hooks/use-context";

export const Body = ()=>{
    const {state} = useContext();
    return (
        <Panel>
            {state.value} {state.panelTab}
        </Panel>
    )
}



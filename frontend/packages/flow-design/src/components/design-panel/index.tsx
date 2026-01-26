import React from "react";
import {DesignPanelProps} from "@/components/design-panel/types";
import {Drawer} from "./drawer";

export const DesignPanel:React.FC<DesignPanelProps>  = (props) =>{

    return (
        <Drawer
            open={props.open}
            onClose={props.onClose}
        />
    )
}

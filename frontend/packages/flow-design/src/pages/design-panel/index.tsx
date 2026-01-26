import React from "react";
import {DesignPanelProps} from "./types";
import {Drawer} from "@/components/drawer";
import {PanelBody} from "@/pages/design-panel/components/body";

export const DesignPanel:React.FC<DesignPanelProps>  = (props) =>{

    return (
        <Drawer
            open={props.open}
            onClose={props.onClose}
        >
            <PanelBody {...props}/>
        </Drawer>
    )
}

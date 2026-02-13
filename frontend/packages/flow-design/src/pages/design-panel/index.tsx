import React from "react";
import {DesignPanelProps} from "./types";
import {Drawer} from "@/components/drawer";
import {DesignPanelLayout} from "@/pages/design-panel/layout";

export const DesignPanel:React.FC<DesignPanelProps>  = (props) =>{

    return (
        <Drawer
            open={props.open}
            onClose={props.onClose}
        >
            <DesignPanelLayout {...props}/>
        </Drawer>
    )
}

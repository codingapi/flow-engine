import React from "react";
import {DesignPanelProps} from "./types";
import {Drawer} from "@/components/drawer";
import {Content} from "@/pages/design-panel/layout/content";

export const DesignPanel:React.FC<DesignPanelProps>  = (props) =>{

    return (
        <Drawer
            open={props.open}
            onClose={props.onClose}
        >
            <Content {...props}/>
        </Drawer>
    )
}

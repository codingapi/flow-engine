import React from "react";
import {DesignPanelProps} from "@/components/design-panel/types";
import {usePresenter} from "@/components/design-panel/hooks/use-presenter";
import {Drawer} from "./drawer";

export const DesignPanel:React.FC<DesignPanelProps>  = (props) =>{

    const {state,presenter} = usePresenter(props);

    return (
        <Drawer
            open={props.open}
            onClose={props.onClose}
            onSave={presenter.save}
        >
            Design Panel
        </Drawer>
    )
}

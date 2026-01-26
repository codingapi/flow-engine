import React from "react";
import {DesignPanelProps} from "@/components/design-panel/types";
import {usePresenter} from "@/components/design-panel/hooks/use-presenter";

export const DesignPanel:React.FC<DesignPanelProps>  = (props) =>{

    const {state,presenter} = usePresenter();

    return (
        <div>
            Design Panel
        </div>
    )
}

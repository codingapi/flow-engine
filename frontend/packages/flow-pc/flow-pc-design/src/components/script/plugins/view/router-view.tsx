import React from "react";
import {RouterViewPlugin, VIEW_KEY} from "@/components/script/plugins/router-view-type";
import {ViewBindPlugin} from "@flow-engine/flow-types";
import {AdvancedScriptEditor} from "@/components/script/components/advanced-script-editor";
import {DEFAULT_ROUTER_SCRIPT} from "@/components/script/default-script";

export const RouterPluginView: React.FC<RouterViewPlugin> = (props) => {
    const TriggerPluginViewComponent = ViewBindPlugin.getInstance().get(VIEW_KEY);
    if(TriggerPluginViewComponent){
        return (
            <TriggerPluginViewComponent {...props} />
        );
    }


    return (
        <AdvancedScriptEditor
            {...props}
            resetScript={()=>{
                return DEFAULT_ROUTER_SCRIPT;
            }}
        />
    );
}

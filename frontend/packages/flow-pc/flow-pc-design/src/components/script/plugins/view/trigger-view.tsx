import React from "react";
import {TriggerViewPlugin, VIEW_KEY} from "@/components/script/plugins/trigger-view-type";
import {ViewBindPlugin} from "@flow-engine/flow-core";
import {AdvancedScriptEditor} from "@/components/script/components/advanced-script-editor";
import {SCRIPT_DEFAULT_TRIGGER} from "@/components/script/default-script";

export const TriggerPluginView: React.FC<TriggerViewPlugin> = (props) => {
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
                return SCRIPT_DEFAULT_TRIGGER;
            }}
        />
    );
}

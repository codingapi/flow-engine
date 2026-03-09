import React from "react";
import {SubProcessViewPlugin, VIEW_KEY} from "@/plugins/sub-process-view-type";
import {ViewBindPlugin} from "@flow-engine/flow-core";
import {SCRIPT_DEFAULT_TRIGGER} from "@/components/script/default-script";
import {AdvancedScriptEditor} from "@/components/script/components/advanced-script-editor";


/**
 * @param props
 * @constructor
 */
export const SubProcessPluginView: React.FC<SubProcessViewPlugin> = (props) => {
    const SubProcessPluginViewComponent = ViewBindPlugin.getInstance().get(VIEW_KEY);
    if(SubProcessPluginViewComponent){
        return (
            <SubProcessPluginViewComponent {...props} />
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

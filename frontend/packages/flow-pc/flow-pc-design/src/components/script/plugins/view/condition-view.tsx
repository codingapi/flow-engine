import React from "react";
import {ConditionViewPlugin, VIEW_KEY} from "@/components/script/plugins/condition-view-type";
import {ViewBindPlugin} from "@flow-engine/flow-types";
import {AdvancedScriptEditor} from "@/components/script/components/advanced-script-editor";
import {DEFAULT_CONDITION_SCRIPT} from "@/components/script/default-script";

/**
 * TODO 条件控制界面
 * @param props
 * @constructor
 */
export const ConditionPluginView: React.FC<ConditionViewPlugin> = (props) => {
    const ConditionPluginViewComponent = ViewBindPlugin.getInstance().get(VIEW_KEY);
    if(ConditionPluginViewComponent){
        return (
            <ConditionPluginViewComponent {...props} />
        );
    }


    return (
        <AdvancedScriptEditor
            {...props}
            resetScript={()=>{
                return DEFAULT_CONDITION_SCRIPT;
            }}
        />
    );
}

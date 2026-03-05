import React from "react";
import {ConditionViewPlugin, VIEW_KEY} from "@/components/script/plugins/condition-view-type";
import {ViewBindPlugin} from "@flow-engine/flow-types";
import {ConditionPanel} from "@/components/script/components/condition/condition-panel";

/**
 * @param props
 * @constructor
 */
export const ConditionPluginView: React.FC<ConditionViewPlugin> = (props) => {
    const ConditionPluginViewComponent = ViewBindPlugin.getInstance().get(VIEW_KEY);

    if (ConditionPluginViewComponent) {
        return (
            <ConditionPluginViewComponent {...props} />
        );
    }

    return (
        <ConditionPanel {...props}/>
    );
}

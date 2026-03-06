import React from "react";
import {ConditionViewPlugin, VIEW_KEY} from "@/components/script/plugins/condition-view-type";
import {ViewBindPlugin} from "@flow-engine/flow-core";
import {ConditionView} from "@/components/script/components/condition";

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
        <ConditionView {...props}/>
    );
}

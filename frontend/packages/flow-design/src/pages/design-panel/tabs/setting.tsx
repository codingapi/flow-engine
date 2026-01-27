import React from "react";
import {Panel} from "@/components/panel";
import {InterferePanel} from "@/pages/design-panel/panels/workflow/interfere";
import {CardForm} from "@/components/form/card";
import {UrgePanel} from "@/pages/design-panel/panels/workflow/urge";


export const TabSetting = ()=>{

    const [form] = CardForm.useForm();

    return (
        <Panel>
            <InterferePanel form={form}/>
            <UrgePanel form={form}/>
        </Panel>
    )
}
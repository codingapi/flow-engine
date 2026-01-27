import React from "react";
import {FormInstance, Switch} from "antd";
import {CardForm} from "@/components/form/card";


interface UrgePanelProps{
    form:FormInstance;
}

export const UrgePanel:React.FC<UrgePanelProps> = (props)=>{
    const form = props.form;

    return (
        <CardForm
            form={form}
            title="催办策略"
        >
            <CardForm.Item
                name={["strategy","urge","enable"]}
                label={"开启"}
                tooltip={"开启催办允许用户点击催办当前审批人"}
            >
                <Switch/>
            </CardForm.Item>
        </CardForm>
    )
}


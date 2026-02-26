import React from "react";
import {FormInstance, Input, Switch} from "antd";
import {CardForm} from "@flow-engine/flow-pc-ui";

interface UrgePanelProps{
    form:FormInstance;
}

export const UrgePanel:React.FC<UrgePanelProps> = (props)=>{

    return (
        <CardForm
            form={props.form}
            title="催办策略"
        >
            <CardForm.Item
                name={["strategies","UrgeStrategy","enable"]}
                label={"开启"}
                tooltip={"开启催办允许用户点击催办当前审批人"}
            >
                <Switch/>
            </CardForm.Item>

            <CardForm.Item
                name={["strategies","UrgeStrategy","interval"]}
                label={"催办间隔时间"}
                tooltip={"每次点击催办的间隔时间，该参数尽在启用时才生效"}
            >
                <Input addonAfter={"单位:秒"}/>
            </CardForm.Item>
        </CardForm>
    )
}


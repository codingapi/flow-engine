import React from "react";
import {FormInstance, Switch} from "antd";
import {CardForm} from "@/components/form/card";

interface InterferePanelProps{
    form:FormInstance;
}

export const InterferePanel:React.FC<InterferePanelProps> = (props)=>{
    return (
        <CardForm
            form={props.form}
            title="干预配置"
        >
            <CardForm.Item
                name={["strategies","InterfereStrategy","enable"]}
                label={"开启"}
                tooltip={"开启干预时可允许流程管理员协助用户操作流程。"}
            >
                <Switch/>
            </CardForm.Item>
        </CardForm>
    )
}


import React from "react";
import {Form, Switch} from "antd";
import {FlowFormFieldMeta} from "@flow-engine/flow-types";

export const FormItemBoolean:React.FC<FlowFormFieldMeta> = (props)=>{

    const rules = props.required?[
        {
            required: props.required,
            message: `${props.name}不能为空`
        }
    ]:[];

    const defaultValue = props.defaultValue === "true" || false;

    return (
        <Form.Item
            name={props.code}
            label={props.name}
            required={props.required}
            rules={rules}
        >
            <Switch defaultValue={defaultValue}/>
        </Form.Item>
    )
}
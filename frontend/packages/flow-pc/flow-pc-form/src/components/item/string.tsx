import React from "react";
import {Form, Input} from "antd";
import {FormField} from "@flow-engine/flow-types";


export const FormItemString:React.FC<FormField> = (props)=>{

    const rules = props.required?[
        {
            required: props.required,
            message: `${props.name}不能为空`
        }
    ]:[];

    return (
        <Form.Item
            name={props.code}
            label={props.name}
            required={props.required}
            rules={rules}
        >
            <Input defaultValue={props.defaultValue} />
        </Form.Item>
    )
}
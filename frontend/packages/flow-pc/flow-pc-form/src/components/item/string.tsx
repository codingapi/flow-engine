import React from "react";
import {Form, Input} from "antd";
import {FormField} from "@flow-engine/flow-types";
import {FormItemProps} from "@/type";


const $Input: React.FC<FormItemProps> = (props) => {

    return (
        <Input
            {...props}
            onChange={(event) => {
                props.onChange?.(event.target.value);
            }}
        />
    )
}


export const FormItemString: React.FC<FormField> = (props) => {

    const rules = props.required ? [
        {
            required: props.required,
            message: `${props.name}不能为空`
        }
    ] : [];


    return (
        <Form.Item
            name={props.code}
            label={props.name}
            required={props.required}
            rules={rules}
            tooltip={props.tooltip}
            help={props.help}
        >
            <$Input
                defaultValue={props.defaultValue}
                placeholder={props.placeholder}
            />
        </Form.Item>
    )
}
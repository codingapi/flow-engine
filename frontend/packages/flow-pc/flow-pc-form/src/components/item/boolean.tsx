import React from "react";
import {Form, Switch} from "antd";
import {FormField} from "@flow-engine/flow-types";
import {FormItemProps} from "@/type";


const $Switch: React.FC<FormItemProps> = (props) => {

    const value = props.value ? props.value === 'true' : undefined;
    const defaultValue = props.defaultValue ? props.defaultValue === 'true' : undefined;

    return (
        <Switch
            value={value}
            defaultValue={defaultValue}
            onChange={(value) => {
                props.onChange?.(value ? 'true' : 'false');
            }}
        />
    )
}

export const FormItemBoolean: React.FC<FormField> = (props) => {

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
            <$Switch
                defaultValue={props.defaultValue}
                placeholder={props.placeholder}
            />
        </Form.Item>
    )
}
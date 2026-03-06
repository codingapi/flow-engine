import React from "react";
import {Form, Input} from "antd";
import {FormItemInputProps, FormItemProps} from "@/type";


const $Input: React.FC<FormItemInputProps> = (props) => {

    const value = props.value || undefined;

    return (
        <Input
            value={value}
            disabled={props.readOnly}
            placeholder={props.placeholder}
            defaultValue={props.defaultValue}
            onChange={(event) => {
                props.onChange?.(event.target.value);
            }}
        />
    )
}


export const FormItemString: React.FC<FormItemProps> = (props) => {

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
                readOnly={props.readOnly}
            />
        </Form.Item>
    )
}
import React from "react";
import {DatePicker, Form} from "antd";
import {FormField} from "@flow-engine/flow-types";
import dayjs from "dayjs";
import {FormItemProps} from "@/type";

const $Date: React.FC<FormItemProps> = (props) => {

    const handlerChange = (value: any) => {
        props.onChange?.(dayjs(value).format('YYYY-MM-DD'));
    }

    const value = props.value ? dayjs(props.value) : undefined;

    return (
        <DatePicker
            {...props}
            value={value as any}
            onChange={handlerChange}
            placeholder={props.placeholder}
        />
    )
}

export const FormItemDate: React.FC<FormField> = (props) => {

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
            <$Date
                defaultValue={props.defaultValue}
                placeholder={props.placeholder}
            />
        </Form.Item>
    )
}
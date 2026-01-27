import React from "react";
import {Card, Form, FormInstance, FormItemProps} from "antd";

interface CardFormProps {
    form?: FormInstance;
    title: string;
    children?: React.ReactNode;
    onChange?: (value: any) => void;
    onFinish?: (value:any) => void;
    initialValue?: any;
}

const CardFormComponent: React.FC<CardFormProps> = (props) => {
    const {form} = props;

    return (
        <Card
            title={props.title}
            variant={'borderless'}
            style={{
                width: '100%',
                margin: 5
            }}
        >
            <Form
                form={form}
                layout="horizontal"
                onValuesChange={props.onChange}
                onFinish={props.onFinish}
                initialValues={props.initialValue}
            >
                {props.children}
            </Form>
        </Card>
    )
}

const CardFormItem: React.FC<FormItemProps> = (props) => {
    return (
        <Form.Item
            labelCol={{
                style: {
                    width: 200
                }
            }}
            {...props}
        >
            {props.children}
        </Form.Item>
    )
}

type FormType = typeof CardFormComponent;
type CardFormComponentType = FormType & {
    Item: typeof CardFormItem;
    useForm: () => [FormInstance];
};

export const CardForm = CardFormComponent as CardFormComponentType;

CardForm.useForm = () => {
    return Form.useForm();
};

CardForm.Item = CardFormItem;
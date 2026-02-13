import { Field, FieldRenderProps } from "@flowgram.ai/fixed-layout-editor";
import { Form,Input } from "antd";
import React from "react";

/**
 * 条件配置
 * @constructor
 */
export const ConditionScript = ()=>{

    const [form] = Form.useForm();

    return (
        <Form
            form={form}
            style={{
                width: '100%',
            }}
            layout="vertical"
        >
            <Form.Item
                label={"条件表达式"}
                name={"script"}
            >
                <Field
                    name={"script"}
                    render={({ field: { value, onChange } }: FieldRenderProps<any>) => (
                        <>
                            <Input value={value} onChange={onChange} />
                        </>
                    )}
                />
            </Form.Item>
        </Form>
    )
}
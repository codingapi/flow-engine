import React from "react";
import {Form,Input} from "antd";
import { Field, FieldRenderProps } from "@flowgram.ai/fixed-layout-editor";

/**
 * 操作人配置策略
 * @constructor
 */
export const OperatorLoadStrategy:React.FC = () => {

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
                label={"当前操作人"}
                name={["OperatorLoadStrategy","script"]}
                tooltip={"支持使用变量"}
            >
                <Field
                    name="OperatorLoadStrategy.script"
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
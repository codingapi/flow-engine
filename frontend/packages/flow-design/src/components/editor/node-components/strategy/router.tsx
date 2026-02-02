import React from "react";
import {Form, Input} from "antd";
import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";

/**
 * 路由策略配置
 * @constructor
 */
export const RouterStrategy:React.FC = () => {
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
                label={"路由表达式"}
                name={["RouterStrategy","script"]}
            >
                <Field
                    name={"RouterStrategy.script"}
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
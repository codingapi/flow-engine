import React from "react";
import {Form, Input} from "antd";
import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";

/**
 * 触发策略配置
 * @constructor
 */
export const TriggerStrategy:React.FC = () => {
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
                label={"触发表达式"}
                name={["TriggerStrategy","script"]}
            >
                <Field
                    name={"TriggerStrategy.script"}
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
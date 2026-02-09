import React from "react";
import {Form,Input} from "antd";
import { Field, FieldRenderProps } from "@flowgram.ai/fixed-layout-editor";

/**
 * 错误触发策略配置(没有匹配到人时)
 * @constructor
 */
export const ErrorTriggerStrategy: React.FC = () => {

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
                label={"异常配置"}
                name={["ErrorTriggerStrategy","script"]}
                tooltip={"在没有匹配到操作人时触发的脚本配置"}
            >
                <Field
                    name={"ErrorTriggerStrategy.script"}
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
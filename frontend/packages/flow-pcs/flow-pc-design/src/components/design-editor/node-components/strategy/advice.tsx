import React from "react";
import {Form, Switch} from "antd";
import { Field, FieldRenderProps } from "@flowgram.ai/fixed-layout-editor";

/**
 * 节点审批意见策略
 * @constructor
 */
export const AdviceStrategy: React.FC = () => {


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
                label={"审批意见为空"}
                name={["AdviceStrategy","adviceNullable"]}
            >
                <Field
                    name="AdviceStrategy.adviceNullable"
                    render={({ field: { value, onChange } }: FieldRenderProps<any>) => (
                        <>
                            <Switch value={value} onChange={onChange} />
                        </>
                    )}
                />
            </Form.Item>

            <Form.Item
                label={"审批签名为空"}
                name={["AdviceStrategy","signable"]}
            >
                <Field
                    name="AdviceStrategy.signable"
                    render={({ field: { value, onChange } }: FieldRenderProps<any>) => (
                        <>
                            <Switch value={value} onChange={onChange} />
                        </>
                    )}
                />
            </Form.Item>
        </Form>
    )
}
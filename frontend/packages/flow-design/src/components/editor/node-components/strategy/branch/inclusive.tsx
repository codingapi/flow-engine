import React from "react";
import {Form, Input} from "antd";
import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";

/**
 * 包容分支策略
 * @constructor
 */
export const InclusiveBranchStrategy = () => {
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
                label={"包容表达式"}
                name={["InclusiveBranchStrategy","script"]}
            >
                <Field
                    name={"InclusiveBranchStrategy.script"}
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
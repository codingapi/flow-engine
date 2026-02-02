import React from "react";
import {Form, Input} from "antd";
import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";

/**
 * 条件分支策略
 * @constructor
 */
export const ConditionBranchStrategy = () => {
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
                name={["ConditionBranchStrategy","script"]}
            >
                <Field
                    name={"ConditionBranchStrategy.script"}
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
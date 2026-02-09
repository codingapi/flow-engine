import React from "react";
import {Form, Input,Switch} from "antd";
import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";

/**
 * 子流程任务策略
 * @constructor
 */
export const SubProcessStrategy:React.FC = () => {
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
                label={"子流程表达式"}
                name={["SubProcessStrategy","script"]}
            >
                <Field
                    name={"SubProcessStrategy.script"}
                    render={({ field: { value, onChange } }: FieldRenderProps<any>) => (
                        <>
                            <Input value={value} onChange={onChange} />
                        </>
                    )}
                />
            </Form.Item>

            <Form.Item
                label={"创建后并提交"}
                name={["SubProcessStrategy","submit"]}
            >
                <Field
                    name={"SubProcessStrategy.submit"}
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
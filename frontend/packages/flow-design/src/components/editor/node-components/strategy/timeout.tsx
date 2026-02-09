import React from "react";
import {Form, Input,Select} from "antd";
import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";

/**
 * 超时策略配置
 * @constructor
 */
export const TimeoutStrategy:React.FC = () => {

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
                label={"超时类型"}
                name={["TimeoutStrategy","type"]}
            >
                <Field
                    name={"TimeoutStrategy.type"}
                    render={({ field: { value, onChange } }: FieldRenderProps<any>) => (
                        <>
                            <Select
                                value={value}
                                onChange={onChange}
                                options={[
                                    { label: '自动提醒', value: 'REMIND' },
                                    { label: '自动同意', value: 'PASS' },
                                    { label: '自动拒绝', value: 'REJECT' },
                                ]}
                            />
                        </>
                    )}
                />
            </Form.Item>

            <Form.Item
                label={"超时时间(分钟)"}
                name={["TimeoutStrategy","timeoutTime"]}
            >
                <Field
                    name={"TimeoutStrategy.timeoutTime"}
                    render={({ field: { value, onChange } }: FieldRenderProps<any>) => (
                        <>
                            <Input
                                value={value}
                                onChange={onChange}
                            />
                        </>
                    )}
                />
            </Form.Item>
        </Form>
    )
}
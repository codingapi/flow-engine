import React from "react";
import {Form, Select} from "antd";
import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";

/**
 * 重新提交策略配置
 * @constructor
 */
export const ResubmitStrategy:React.FC = () => {

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
                label={"重新提交配置"}
                name={["ResubmitStrategy", "type"]}
            >
                <Field
                    name={"ResubmitStrategy.type"}
                    render={({field: {value, onChange}}: FieldRenderProps<any>) => {
                        return (
                            <Select
                                value={value}
                                onChange={onChange}
                                options={[
                                    {
                                        label: '跳转到撤回节点',
                                        value: 'RESUME'
                                    },
                                    {
                                        label: '从头逐级提交',
                                        value: 'CHAIN'
                                    },
                                ]}
                            />
                        )
                    }}
                />

            </Form.Item>

        </Form>
    )
}
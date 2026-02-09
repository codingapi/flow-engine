import React from "react";
import {Form, Select} from "antd";
import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";

/**
 * 提交人与审批人一致配置
 * @constructor
 */
export const SameOperatorAuditStrategy:React.FC = () => {

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
                label={"相同人员提交配置"}
                name={["SameOperatorAuditStrategy", "type"]}
            >
                <Field
                    name={"SameOperatorAuditStrategy.type"}
                    render={({field: {value, onChange}}: FieldRenderProps<any>) => {
                        return (
                            <Select
                                value={value}
                                onChange={onChange}
                                options={[
                                    {
                                        label: '自动通过',
                                        value: 'AUTO_PASS'
                                    },
                                    {
                                        label: '手动审批',
                                        value: 'MANUAL_PASS'
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
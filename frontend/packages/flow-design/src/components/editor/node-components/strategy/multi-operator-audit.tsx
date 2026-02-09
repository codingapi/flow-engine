import React from "react";
import {Form, Input, Select} from "antd";
import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";

/**
 * 多人审批策略配置
 * @constructor
 */
export const MultiOperatorAuditStrategy: React.FC = () => {

    const [form] = Form.useForm();

    const [percentVisible, setPercentVisible] = React.useState(false);

    return (
        <Form
            form={form}
            style={{
                width: '100%',
            }}
            layout="vertical"
        >
            <Form.Item
                label={"多人审批策略"}
                name={["MultiOperatorAuditStrategy", "type"]}
            >
                <Field
                    name={"MultiOperatorAuditStrategy.type"}
                    render={({field: {value, onChange}}: FieldRenderProps<any>) => {
                        setPercentVisible(value === 'MERGE');
                        return (
                            <Select
                                value={value}
                                onChange={onChange}
                                options={[
                                    {
                                        label: '循序提交',
                                        value: 'SEQUENCE'
                                    },
                                    {
                                        label: '合并审核',
                                        value: 'MERGE'
                                    },
                                    {
                                        label: '任意审核',
                                        value: 'ANY'
                                    },
                                    {
                                        label: '任意一人',
                                        value: 'RANDOM_ONE'
                                    }
                                ]}
                            />
                        )
                    }}
                />

            </Form.Item>

            {percentVisible && (
                <Form.Item
                    label={"会签通过百分比"}
                    name={["MultiOperatorAuditStrategy", "percent"]}
                >
                    <Field
                        name={"MultiOperatorAuditStrategy.percent"}
                        render={({field: {value, onChange}}: FieldRenderProps<any>) => (
                            <>
                                <Input value={value} onChange={onChange} type="number"/>
                            </>
                        )}
                    />
                </Form.Item>
            )}
        </Form>
    )
}
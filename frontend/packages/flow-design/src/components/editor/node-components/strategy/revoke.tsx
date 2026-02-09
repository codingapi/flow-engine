import React from "react";
import {Form, Switch,Select} from "antd";
import { Field, FieldRenderProps } from "@flowgram.ai/fixed-layout-editor";

/**
 * 撤回策略
 * @constructor
 */
export const RevokeStrategy:React.FC = () => {

    const [form] = Form.useForm();
    const [enable, setEnable] = React.useState<boolean>(false);

    return (
        <Form
            form={form}
            style={{
                width: '100%',
            }}
            layout="vertical"
        >
            <Form.Item
                label={"启动撤回功能"}
                name={["RevokeStrategy","enable"]}
            >
                <Field
                    name="RevokeStrategy.enable"
                    render={({ field: { value, onChange } }: FieldRenderProps<any>) => {
                        setEnable(value);
                        return (
                            <Switch value={value} onChange={onChange} />
                        )
                    }}
                />
            </Form.Item>
            {enable && (
                <Form.Item
                    label={"撤回方式"}
                    name={["RevokeStrategy","type"]}
                >
                    <Field
                        name="RevokeStrategy.type"
                        render={({ field: { value, onChange } }: FieldRenderProps<any>) => {
                            return (
                                <Select
                                    value={value}
                                    onChange={onChange}
                                    options={[
                                        {
                                            label: '撤回上级节点',
                                            value: 'REVOKE_NEXT'
                                        },
                                        {
                                            label: '撤回到当前节点',
                                            value: 'REVOKE_CURRENT'
                                        },
                                    ]}
                                />
                            )
                        }}
                    />
                </Form.Item>
            )}
        </Form>
    )
}
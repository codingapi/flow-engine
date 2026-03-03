import React from "react";
import {Button, Form, Input, Space} from "antd";
import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";
import {GroovyScriptPreview} from "@/components/script/components/groovy-script-preview";
import { EditOutlined } from "@ant-design/icons";

/**
 * 触发策略配置
 * @constructor
 */
export const TriggerStrategy:React.FC = () => {
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
                label={"触发表达式"}
                name={["TriggerStrategy","script"]}
            >
                <Field
                    name={"TriggerStrategy.script"}
                    render={({ field: { value, onChange } }: FieldRenderProps<any>) => (
                        <Space.Compact style={{width: '100%'}}>
                            <GroovyScriptPreview
                                script={value}
                            />

                            <Button
                                icon={<EditOutlined/>}
                                onClick={() => {
                                }}
                                style={{borderRadius: '0 6px 6px 0'}}
                            >
                                编辑
                            </Button>
                        </Space.Compact>
                    )}
                />
            </Form.Item>
        </Form>
    )
}
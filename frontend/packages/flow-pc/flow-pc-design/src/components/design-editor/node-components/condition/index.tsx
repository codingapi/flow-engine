import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";
import {Button, Form, Space} from "antd";
import React from "react";
import {GroovyScriptPreview} from "@/components/script/components/groovy-script-preview";
import {EditOutlined} from "@ant-design/icons";

/**
 * 条件配置
 * @constructor
 */
export const ConditionScript = ()=>{

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
                name={"script"}
            >
                <Field
                    name={"script"}
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
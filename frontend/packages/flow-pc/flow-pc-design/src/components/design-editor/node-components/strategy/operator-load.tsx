import React from "react";
import {Button, Form, Space} from "antd";
import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";
import {EditOutlined} from "@ant-design/icons";
import {GroovyScriptPreview} from "@/components/script/components/groovy-script-preview";

/**
 * 操作人配置策略
 * @constructor
 */
export const OperatorLoadStrategy:React.FC = () => {

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
                label={"当前操作人"}
                name={["OperatorLoadStrategy","script"]}
                tooltip={"设定流程的审批人"}
            >
                <Field
                    name="OperatorLoadStrategy.script"
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
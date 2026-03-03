import React from "react";
import {Button, Form, Input,Space,Switch} from "antd";
import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";
import {GroovyScriptPreview} from "@/components/script/components/groovy-script-preview";
import { EditOutlined } from "@ant-design/icons";

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
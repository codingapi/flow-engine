import React from "react";
import {Button, Form, Space} from "antd";
import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";
import {GroovyScriptPreview} from "@/components/script/components/groovy-script-preview";
import {EditOutlined} from "@ant-design/icons";
import {ErrorTriggerConfigModal} from "@/components/script/modal/error-trigger-config-modal";

/**
 * 错误触发策略配置(没有匹配到人时)
 * @constructor
 */
export const ErrorTriggerStrategy: React.FC = () => {

    const [form] = Form.useForm();
    const [visible,setVisible] = React.useState(false);

    return (
        <Form
            form={form}
            style={{
                width: '100%',
            }}
            layout="vertical"
        >
            <Form.Item
                label={"异常配置"}
                name={["ErrorTriggerStrategy","script"]}
                tooltip={"在没有匹配到操作人时触发的脚本配置"}
            >
                <Field
                    name={"ErrorTriggerStrategy.script"}
                    render={({ field: { value, onChange } }: FieldRenderProps<any>) => (
                        <Space.Compact style={{width: '100%'}}>
                            <GroovyScriptPreview
                                script={value}
                            />

                            <Button
                                icon={<EditOutlined/>}
                                onClick={() => {
                                    setVisible(true);
                                }}
                                style={{borderRadius: '0 6px 6px 0'}}
                            >
                                编辑
                            </Button>

                            <ErrorTriggerConfigModal
                                open={visible}
                                onCancel={()=>{setVisible(false);}}
                                onConfirm={(value)=>{onChange(value)}}
                                script={value}
                            />
                        </Space.Compact>
                    )}
                />
            </Form.Item>
        </Form>
    )
}
import React from "react";
import {Button, Form, Space} from "antd";
import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";
import {GroovyScriptPreview} from "@/components/script/components/groovy-script-preview";
import {EditOutlined} from "@ant-design/icons";
import {TriggerConfigModal} from "@/components/script/modal/trigger-config-modal";

/**
 * 触发策略配置
 * @constructor
 */
export const TriggerStrategy:React.FC = () => {
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
                                    setVisible(true);
                                }}
                                style={{borderRadius: '0 6px 6px 0'}}
                            >
                                编辑
                            </Button>

                            <TriggerConfigModal
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
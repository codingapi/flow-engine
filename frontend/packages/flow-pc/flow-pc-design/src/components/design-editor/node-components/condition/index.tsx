import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";
import {Button, Form, Space} from "antd";
import React from "react";
import {GroovyScriptPreview} from "@/components/script/components/groovy-script-preview";
import {EditOutlined} from "@ant-design/icons";
import {ConditionConfigModal} from "@/components/script/modal/condition-config-modal";

/**
 * 条件配置
 * @constructor
 */
export const ConditionScript = ()=>{

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
                                    setVisible(true);
                                }}
                                style={{borderRadius: '0 6px 6px 0'}}
                            >
                                编辑
                            </Button>

                            <ConditionConfigModal
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
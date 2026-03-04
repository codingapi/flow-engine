import React from "react";
import {Button, Form, Space} from "antd";
import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";
import {GroovyScriptPreview} from "@/components/script/components/groovy-script-preview";
import {EditOutlined} from "@ant-design/icons";
import {RouterConfigModal} from "@/components/script/modal/router-config-modal";

/**
 * 路由策略配置
 * @constructor
 */
export const RouterStrategy:React.FC = () => {
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
                label={"路由表达式"}
                name={["RouterStrategy","script"]}
            >
                <Field
                    name={"RouterStrategy.script"}
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

                            <RouterConfigModal
                                open={visible}
                                script={value}
                                onCancel={()=>{setVisible(false);}}
                                onConfirm={(value)=>{
                                    onChange(value);
                                }}
                            />
                        </Space.Compact>
                    )}
                />
            </Form.Item>
        </Form>
    )
}
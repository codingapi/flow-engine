import {Form, FormInstance, Input, Modal, Select} from "antd";
import React from "react";
import {GroovyCodeEditor} from "@/components/groovy-code";
import {GroovyScriptConvertorUtil} from "@flow-engine/flow-core";

interface ActionModalProps {
    open: boolean;
    onCancel: () => void;
    form: FormInstance<any>;
    onFinish: (values: any) => void;
    custom: boolean;
    options:any[];
}

export const ActionModal: React.FC<ActionModalProps> = (props) => {
    const custom = props.custom;


    const handleChangeNodeType = (value:string)=>{
        const script = props.form.getFieldValue('script') as string;
        const returnData = GroovyScriptConvertorUtil.getReturnScript(script).trim();
        let groovy;
        if(returnData){
            groovy = script.replace(returnData,`'${value}'`);
        }else {
            groovy = `// 自定义脚本，返回的数据为动作类型
            // @SCRIPT_META {"trigger":"${value}"}
            def run(request){
                return '${value}';
            }
            `
        }
        props.form.setFieldValue("script", GroovyScriptConvertorUtil.formatScript(groovy));
    }

    return (
        <Modal
            width={"60%"}
            open={props.open}
            onCancel={props.onCancel}
            title={custom ? "自定义按钮" : "编辑动作"}
            destroyOnHidden={true}
            onOk={() => {
                props.form.submit();
            }}
        >
            <Form
                form={props.form}
                layout="vertical"
                onFinish={(values) => {
                    props.onFinish(values);
                    props.onCancel();
                }}
            >
                <Form.Item
                    name={"id"}
                    hidden={true}
                >
                    <Input/>
                </Form.Item>
                <Form.Item
                    name={"title"}
                    label={"按钮名称"}
                    required={true}
                    rules={[
                        {
                            required: true,
                            message: '按钮名称不能为空'
                        }
                    ]}
                >
                    <Input placeholder={"请输入按钮名称"}/>
                </Form.Item>

                <Form.Item
                    name={"icon"}
                    label={"按钮图标"}
                >
                    <Input placeholder={"请输入按钮图标"}/>
                </Form.Item>

                <Form.Item
                    name={"style"}
                    label={"按钮样式"}
                >
                    <Input placeholder={"请输入按钮样式"}/>
                </Form.Item>

                {custom && (
                    <>
                        <Form.Item
                            name={"trigger"}
                            label={"触发动作"}
                            required={true}
                            rules={[
                                {
                                    required: true,
                                    message: '触发动作不能为空'
                                }
                            ]}
                        >
                            <Select
                                placeholder={"请选择触发动作类型"}
                                options={props.options}
                                onChange={handleChangeNodeType}
                            />
                        </Form.Item>

                        <Form.Item
                            name={"script"}
                            label={"自定义脚本"}
                            required={true}
                            rules={[
                                {
                                    required: true,
                                    message: '自定义脚本不能为空'
                                }
                            ]}
                        >
                            <GroovyCodeEditor
                                placeholder={"请输入自定义脚本"}
                                options={{
                                    minHeight:200
                                }}
                            />
                        </Form.Item>
                    </>
                )}

            </Form>
        </Modal>
    )
}
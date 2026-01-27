import React from "react";
import { Input } from "antd";
import {Panel} from "@/components/panel";
import {CardForm} from "@/components/form/card";
import {useDesignContext} from "../hooks/use-design-context";


export const TabBase = ()=>{

    const [baseForm] = CardForm.useForm();
    const [operatorForm] = CardForm.useForm();
    const {state,context} = useDesignContext();

    const formActionContext = context.getFormActionContext();

    React.useEffect(()=>{
        baseForm.setFieldsValue(state.workflow);
        operatorForm.setFieldsValue(state.workflow);
    },[]);


    // 注册form行为
    React.useEffect(()=>{
        formActionContext.addAction({
            save() {
                return baseForm.getFieldsValue();
            }
        });

        formActionContext.addAction({
            save() {
                return operatorForm.getFieldsValue();
            }
        });
    },[]);

    React.useEffect(()=>{
        baseForm.setFieldsValue(state.workflow);
        operatorForm.setFieldsValue(state.workflow);
    },[state.workflow]);

    return (
        <Panel>
            <CardForm
                form={baseForm}
                title="基本信息"
                onFinish={(values)=>{
                    console.log(values);
                }}
            >
                <CardForm.Item
                    name={"title"}
                    label={"流程名称"}
                    rules={[
                        {
                            required: true,
                            message: '请输入流程名称'
                        }
                    ]}
                >
                    <Input placeholder={"请输入流程名称"}/>
                </CardForm.Item>


                <CardForm.Item
                    name={"code"}
                    label={"流程编码"}
                    rules={[
                        {
                            required: true,
                            message: '请输入流程编码'
                        }
                    ]}
                >
                    <Input placeholder={"请输入流程编码"}/>
                </CardForm.Item>

                <CardForm.Item
                    name={["form","name"]}
                    label={"表单名称"}
                    tooltip={"表单名称是主表的名称"}
                    rules={[
                        {
                            required: true,
                            message: '请输入表单名称'
                        }
                    ]}
                >
                    <Input placeholder={"请输入表单名称"}/>
                </CardForm.Item>

                <CardForm.Item
                    name={["form","code"]}
                    label={"表单编码"}
                    tooltip={"表单编码是主表的编码"}
                    rules={[
                        {
                            required: true,
                            message: '请输入表单编码'
                        }
                    ]}
                >
                    <Input placeholder={"请输入表单编码"}/>
                </CardForm.Item>

            </CardForm>

            <CardForm
                form={operatorForm}
                title="发起配置"
                onFinish={(values)=>{
                    console.log(values);
                }}
            >
                <CardForm.Item
                    name={"createdOperator"}
                    label={"发起人范围"}
                    rules={[
                        {
                            required: true,
                            message: '请输入发起人范围'
                        }
                    ]}
                >
                    <Input placeholder={"请输入发起人范围"}/>
                </CardForm.Item>
            </CardForm>
        </Panel>
    )
}
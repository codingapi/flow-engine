import React from "react";
import {Panel} from "@/components/panel";
import {CardForm} from "@/components/form/card";
import { Input } from "antd";


export const TabBase = ()=>{

    const [form] = CardForm.useForm();

    return (
        <Panel>
            <CardForm
                form={form}
                title="基本信息"
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
                    name={"formName"}
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
                    name={"formCode"}
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
                form={form}
                title="发起配置"
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
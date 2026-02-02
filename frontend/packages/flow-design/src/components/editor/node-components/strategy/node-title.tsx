import React from "react";
import {Form, Input} from "antd";
import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";

/**
 * 节点标题策略配置
 * @constructor
 */
export const NodeTitleStrategy: React.FC = () => {

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
                label={"标题表达式"}
                name={"script"}
                tooltip={"支持使用变量"}
            >
               <Field
                   name="script"
                   render={({ field: { value, onChange } }: FieldRenderProps<any>) => (
                       <>
                           <Input value={value} onChange={onChange} />
                       </>
                   )}
               />
            </Form.Item>
        </Form>
    )
}
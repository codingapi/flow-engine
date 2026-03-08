import React from "react";
import {ActionFormProps} from "@/components/script/typings";
import { Form,Input,Row,Col } from "antd";
import {ActionIcon} from "./icon";
import {ActionStyle} from "./style";
import {ActionFactory} from "@/components/script/components/action/components/factory";


export const ActionForm:React.FC<ActionFormProps> = (props)=>{

    const type = props.form.getFieldValue("type");

    const FormAction = ActionFactory.getInstance().getActionForm(type);

    return (
        <Form
            form={props.form}
            layout="vertical"
            onFinish={(values) => {
                props.onFinish(values);
            }}
        >
            <Form.Item
                name={"id"}
                hidden={true}
            >
                <Input/>
            </Form.Item>
            <Row gutter={[8, 8]}>
                <Col span={12}>
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
                </Col>
                <Col span={12}>
                    <Form.Item
                        name={"icon"}
                        label={"按钮图标"}
                    >
                        <ActionIcon/>
                    </Form.Item>
                </Col>
                <Col span={24}>
                    <Form.Item
                        name={"style"}
                        label={"按钮样式"}
                    >
                        <ActionStyle/>
                    </Form.Item>
                </Col>
            </Row>

            {FormAction && (
                <FormAction {...props}/>
            )}
        </Form>
    )
}
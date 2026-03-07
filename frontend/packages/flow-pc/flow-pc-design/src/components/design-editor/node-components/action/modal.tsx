import {Col, Form, FormInstance, Input, Modal, Row} from "antd";
import React from "react";
import {CustomScriptView} from "./script";
import {ActionStyle} from "./style";
import {ActionIcon} from "@/components/design-editor/node-components/action/icon";

interface ActionModalProps {
    open: boolean;
    onCancel: () => void;
    form: FormInstance<any>;
    onFinish: (values: any) => void;
    custom: boolean;
    options: any[];
}


export const ActionModal: React.FC<ActionModalProps> = (props) => {
    const custom = props.custom;

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

                    {custom && (
                        <CustomScriptView options={props.options} form={props.form}/>
                    )}
                </Row>

            </Form>
        </Modal>
    )
}
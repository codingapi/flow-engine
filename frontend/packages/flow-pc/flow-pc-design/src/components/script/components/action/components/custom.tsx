import React from "react";
import {ActionFormProps} from "@/components/script/typings";
import {Col, Form, Row} from "antd";
import {ConditionCustomView} from "@/components/script/plugins/view/action-custom-view";

export const CustomActionForm: React.FC<ActionFormProps> = (props) => {

    const actionOptionTypes = props.manager.getCurrentNodeActionOptions();

    return (
        <Row>
            <Col span={24}>
                <Form.Item
                    name={"script"}
                    label={"自定义脚本"}
                    required={true}
                    help={"请先设置触发动作类型"}
                    rules={[
                        {
                            required: true,
                            message: '自定义脚本不能为空'
                        }
                    ]}
                >
                    <ConditionCustomView
                        options={actionOptionTypes}
                    />
                </Form.Item>
            </Col>
        </Row>
    )
}
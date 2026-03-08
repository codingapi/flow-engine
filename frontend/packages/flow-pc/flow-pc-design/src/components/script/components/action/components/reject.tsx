import React from "react";
import {ActionFormProps} from "@/components/script/typings";
import {Col, Form, Row} from "antd";
import {ConditionRejectView} from "@/components/script/plugins/view/action-reject-view";


export const RejectActionForm:React.FC<ActionFormProps> = (props)=>{


    return (
        <Row>
            <Col span={24}>
                <Form.Item
                    name={"script"}
                    label={"拒绝策略"}
                >
                    <ConditionRejectView/>
                </Form.Item>
            </Col>
        </Row>
    )
}
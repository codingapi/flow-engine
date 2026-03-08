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
                    tooltip={"退回的节点,当选择终止时则直接结束当前流程"}
                    label={"拒绝策略"}
                    help={"配置拒绝时跳转的节点"}
                >
                    <ConditionRejectView
                        nodeId={props.nodeId}
                    />
                </Form.Item>
            </Col>
        </Row>
    )
}
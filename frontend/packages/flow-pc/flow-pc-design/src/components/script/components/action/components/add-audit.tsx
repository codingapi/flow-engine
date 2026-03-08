import React from "react";
import {ActionFormProps} from "@/components/script/typings";
import {Col, Form, Row} from "antd";
import {OperatorLoadPluginView} from "@/components/script/plugins/view/operator-load-view";


interface AddAuditInputProps{
    value?:string;
    onChange?:(value:string) => void;
}

const AddAuditInput:React.FC<AddAuditInputProps> = (props)=>{

    const script = props.value || '';

    const handleChange = (value:string)=>{
        props.onChange && props.onChange(value);
    }

    return (
        <OperatorLoadPluginView
            script={script}
            onChange={handleChange}
        />
    )
}

export const AddAuditActionForm:React.FC<ActionFormProps> = (props)=>{

    return (
        <Row>
            <Col span={24}>
                <Form.Item
                    name={"script"}
                    label={"加签人员范围"}
                    help={"加签人员范围为空时，即为全部人员"}
                >
                    <AddAuditInput/>
                </Form.Item>
            </Col>
        </Row>
    )
}
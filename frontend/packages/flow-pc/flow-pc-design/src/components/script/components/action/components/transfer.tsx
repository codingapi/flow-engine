import React from "react";
import {ActionFormProps} from "@/components/script/typings";
import {OperatorLoadPluginView} from "@/components/script/plugins/view/operator-load-view";
import {Col, Form, Row } from "antd";



interface TransferInputProps{
    value?:string;
    onChange?:(value:string) => void;
}

const TransferInput:React.FC<TransferInputProps> = (props)=>{

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


export const TransferActionForm:React.FC<ActionFormProps> = (props)=>{

    return (
        <Row>
            <Col span={24}>
                <Form.Item
                    name={"script"}
                    label={"转办人员范围"}
                    help={"转办人员范围为空时，即为全部人员"}
                >
                    <TransferInput/>
                </Form.Item>
            </Col>
        </Row>
    )
}
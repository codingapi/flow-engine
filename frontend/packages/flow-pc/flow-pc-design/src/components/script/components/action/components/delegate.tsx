import React from "react";
import {ActionFormProps} from "@/components/script/typings";
import {OperatorLoadPluginView} from "@/plugins/view/operator-load-view";
import {Col, Form, Row } from "antd";



interface DelegateInputProps{
    value?:string;
    onChange?:(value:string) => void;
}

const DelegateInput:React.FC<DelegateInputProps> = (props)=>{

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

export const DelegateActionForm:React.FC<ActionFormProps> = (props)=>{

    return (
        <Row>
            <Col span={24}>
                <Form.Item
                    name={"script"}
                    label={"委派人员范围"}
                    help={"委派人员范围为空时，即为全部人员"}
                >
                    <DelegateInput/>
                </Form.Item>
            </Col>
        </Row>
    )
}
import React from "react";
import {type ViewComponentProps} from "@flow-engine/flow-design";
import {Form, Input} from "antd";

export const LeaveView: React.FC<ViewComponentProps> = (props) => {
    return (
        <Form
            form={props.form}
            layout={"vertical"}
        >
            <Form.Item
                name={"days"}
                label={"请假天数"}
            >
                <Input/>
            </Form.Item>

            <Form.Item
                name={"desc"}
                label={"请假理由"}
            >
                <Input/>
            </Form.Item>
        </Form>
    )
};
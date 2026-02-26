import React from "react";
import {type ViewComponentProps} from "@flow-engine/flow-types";
import {Form, Input} from "antd";
import {ObjectUtils} from "@flow-engine/flow-core";

export const FlowFormView: React.FC<ViewComponentProps> = (props) => {

    const [values, setValues] = React.useState<any>({});

    const form = props.form;

    return (
        <Form
            form={form as any}
            layout={"vertical"}
            onBlur={()=>{
                const latestValues = form.getFieldsValue();
                if(ObjectUtils.isEqual(values,latestValues)){
                    return;
                }
                setValues(latestValues);
                props.onValuesChange?.(latestValues);
            }}
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
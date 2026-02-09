import React from "react";
import {Form, Switch} from "antd";
import { Field, FieldRenderProps } from "@flowgram.ai/fixed-layout-editor";

/**
 * 记录合并策略配置
 * @constructor
 */
export const RecordMergeStrategy:React.FC = () => {


    const [form] = Form.useForm();

    return (
        <Form
            form={form}
            style={{
                width: '100%',
            }}
            layout="vertical"
        >
            <Form.Item
                label={"启用记录合并"}
                name={["RecordMergeStrategy","enable"]}
            >
                <Field
                    name="RecordMergeStrategy.enable"
                    render={({ field: { value, onChange } }: FieldRenderProps<any>) => (
                        <>
                            <Switch value={value} onChange={onChange} />
                        </>
                    )}
                />
            </Form.Item>
        </Form>
    )
}
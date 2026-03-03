import React from "react";
import {OperatorCreateViewPlugin, VIEW_KEY} from "@/components/script/plugins/operator-create-view-type";
import { Form, Select } from "antd";
import {ViewBindPlugin} from "@flow-engine/flow-types";
import {DEFAULT_OPERATOR_CREATE_SCRIPT} from "@/components/script/default-script";

export const OperatorCreatePluginView: React.FC<OperatorCreateViewPlugin> = (props) => {

    const OperatorCreatePluginViewComponent = ViewBindPlugin.getInstance().get(VIEW_KEY);

    if(OperatorCreatePluginViewComponent){
        return (
            <OperatorCreatePluginViewComponent {...props} />
        );
    }

    return (
        <Form>
            <Form.Item
                name={"type"}
                label={"类型"}
                tooltip={"选择人员类型"}
            >
                <Select
                    options={[
                        {
                            label:'任意用户',
                            value:'1',
                        }
                    ]}
                    placeholder={"请选择人员类型"}
                    onChange={(value)=>{
                        props.onChange(DEFAULT_OPERATOR_CREATE_SCRIPT);
                    }}
                />
            </Form.Item>
        </Form>
    )
}
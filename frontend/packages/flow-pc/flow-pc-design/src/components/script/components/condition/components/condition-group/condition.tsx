import {Condition} from "@/components/script/components/condition/typings";
import {Input, Select, Space} from "antd";
import React from "react";

interface ConditionItemViewProps {
    data?: Condition;
}

export const ConditionItemView: React.FC<ConditionItemViewProps> = (props) => {
    const label = props.data?.label || '未设置';
    const [type,setType] = React.useState('variable');

    return (
        <Space.Compact>
            <Select
                style={{ width: "100px" }}
                defaultValue={type}
                options={[
                    {
                        label: '插入参数',
                        value: 'variable'
                    },
                    {
                        label: '输入参数',
                        value: 'input'
                    }
                ]}
                onChange={(value) => {
                    setType(value);
                }}
            />
            {type === "variable" && (
                <Select
                    style={{ width: "100px" }}
                />
            )}

            {type === "input" && (
                <Input
                    style={{ width: "100px" }}
                />
            )}

        </Space.Compact>
    )
}
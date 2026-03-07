import {Condition} from "@/components/script/components/condition/typings";
import {Input, Select, Space} from "antd";
import React from "react";
import {useConditionContext} from "@/components/script/components/condition/hooks/use-condition-context";
import {GroovyVariableMapping} from "@/components/script/typings";

interface ConditionItemViewProps {
    data?: Condition;
    id: string;
    location: 'left' | 'right';
}

interface ConditionInputProps {
    value: string;
    onChange: (value: Condition) => void;
}

const VariableSelect: React.FC<ConditionInputProps> = (props) => {

    const {state} = useConditionContext();

    const variables = React.useMemo(() => {
        const groups = new Map<string, GroovyVariableMapping[]>();

        for (const mapping of state.variables) {
            const existing = groups.get(mapping.tag) || [];
            existing.push(mapping);
            groups.set(mapping.tag, existing);
        }

        const options: any[] = [];
        for (const group of Array.from(groups.keys())) {
            const children = groups.get(group) || [];

            options.push({
                label: group,
                options: children,
            })
        }

        return options;
    }, [state.variables]);

    const handleChange = (value: string, option: any) => {
        props.onChange({
            label: option.label,
            value: option.value,
            dataType: option.type,
            type: 'variable',
        });
    }

    return (
        <Select
            showSearch={{
                filterOption: (value: string, option) => {
                    if (option.value) {
                        return option.value.includes(value) || option.label.includes(value);
                    }
                    return false;
                }
            }}
            value={props.value}
            placeholder={"请选择参数"}
            style={{width: "200px"}}
            options={variables}
            onChange={handleChange}
        />
    )
}

const TypeInput: React.FC<ConditionInputProps> = (props) => {

    const handleChange = (value: string) => {
        props.onChange({
            label: value,
            value: value,
            dataType: 'STRING',
            type: 'input',
        });
    }

    return (
        <Input
            style={{width: "200px"}}
            value={props.value}
            placeholder={"请输入参数"}
            onChange={(event)=>{
                handleChange(event.target.value);
            }}
        />
    )
}

export const ConditionItemView: React.FC<ConditionItemViewProps> = (props) => {
    const [type, setType] = React.useState(props.data?.type || 'variable');

    const {context} = useConditionContext();

    const presenter =  context.getPresenter().getConditionGroupPresenter();

    const handleUpdate = (value: any) => {
        const latest = {
            ...props.data,
            ...value
        }
        presenter.updateCondition(props.id,props.location, latest);
    }

    React.useEffect(()=>{
        if(props.data) {
            setType(props.data.type);
        }
    },[props.data])

    return (
        <Space.Compact>
            <Select
                style={{width: "100px"}}
                value={type}
                options={[
                    {
                        label: '选择变量',
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
                <VariableSelect
                    value={props.data?.value || ''}
                    onChange={(value) => {
                        handleUpdate(value)
                    }}
                />
            )}

            {type === "input" && (
                <TypeInput
                    value={props.data?.value || ''}
                    onChange={(value) => {
                        handleUpdate(value)
                    }}
                />
            )}

        </Space.Compact>
    )
}
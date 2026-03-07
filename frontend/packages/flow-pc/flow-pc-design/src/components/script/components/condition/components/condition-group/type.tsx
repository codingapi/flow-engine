import {RelationType, relationTypeOptions} from "@/components/script/components/condition/typings";
import {Select} from "antd";
import React from "react";
import {useConditionContext} from "@/components/script/components/condition/hooks/use-condition-context";

interface ConditionTypeViewProps {
    type?: RelationType;
    id: string;
}

export const ConditionTypeView: React.FC<ConditionTypeViewProps> = (props) => {
    const type = props.type;

    const {context} = useConditionContext();
    const presenter = context.getPresenter().getConditionGroupPresenter();

    const handleChangeType = (value: string) => {
        presenter.updateType(props.id, value as RelationType);
    }

    return (
        <Select
            placeholder={"请选择关系"}
            options={relationTypeOptions}
            value={type}
            style={{width: "100px"}}
            onChange={handleChangeType}
        />
    )
}
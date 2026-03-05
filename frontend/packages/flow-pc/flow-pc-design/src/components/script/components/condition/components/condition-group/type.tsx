import {RelationType, relationTypeOptions} from "@/components/script/components/condition/typings";
import React from "react";

interface ConditionTypeViewProps {
    type?: RelationType;
}

export const ConditionTypeView: React.FC<ConditionTypeViewProps> = (props) => {
    const type = props.type;
    const typeLabel = relationTypeOptions.find(item => item.value === type);
    if (typeLabel) {
        return (
            <span>{typeLabel.label}</span>
        )
    }
    return (
        <span>未知</span>
    )
}
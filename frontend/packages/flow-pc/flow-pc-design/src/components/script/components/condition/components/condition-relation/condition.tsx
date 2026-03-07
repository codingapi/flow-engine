import React from "react";
import {ConditionRelationProps} from "@/components/script/components/condition/typings";
import {
    useDropdownMenus
} from "@/components/script/components/condition/components/condition-relation/hooks/use-dropdown-menus";
import {Dropdown, Tag} from "antd";

export const RelationCondition: React.FC<ConditionRelationProps> = (props) => {
    const items = useDropdownMenus(props);
    return (
        <Dropdown
            menu={{items}}>
            <Tag style={{
                cursor: "pointer",
            }}>
                {props.current.label}
            </Tag>
        </Dropdown>
    )
}
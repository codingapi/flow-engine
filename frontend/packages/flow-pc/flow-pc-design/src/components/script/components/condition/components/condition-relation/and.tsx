import React from "react";
import {ConditionRelationProps} from "@/components/script/components/condition/typings";
import {Badge, Dropdown, Tag} from "antd";
import {
    useDropdownMenus
} from "@/components/script/components/condition/components/condition-relation/hooks/use-dropdown-menus";
import {
    RelationValidate
} from "@/components/script/components/condition/components/condition-relation/domain/relation-validate";

export const RelationAnd: React.FC<ConditionRelationProps> = (props) => {
    const items = useDropdownMenus(props);
    const relationValidate = new RelationValidate(props);

    return (
        <Dropdown
            menu={{items}}>
            <Badge
                dot={relationValidate.isError()}
            >
                <Tag
                    style={{
                        cursor: "pointer",
                    }}
                >
                    {props.current.label}
                </Tag>
            </Badge>
        </Dropdown>
    )
}
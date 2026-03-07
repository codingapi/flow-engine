import {Badge, Dropdown, Tag} from "antd";
import React from "react";
import {ConditionRelationProps} from "@/components/script/components/condition/typings";
import {
    useDropdownMenus
} from "@/components/script/components/condition/components/condition-relation/hooks/use-dropdown-menus";
import {RelationRender} from "@/components/script/components/condition/components/condition-relation/render";
import {
    RelationValidate
} from "@/components/script/components/condition/components/condition-relation/domain/relation-validate";

export const RelationGroup: React.FC<ConditionRelationProps> = (props) => {
    const items = useDropdownMenus(props);
    const relationValidate = new RelationValidate(props);

    return (
        <>
            <Badge
                dot={relationValidate.isError()}
            >
                <Dropdown
                    menu={{items}}>
                    <Tag
                        style={{
                            cursor: "pointer",
                        }}
                    >（</Tag>
                </Dropdown>
            </Badge>
            {RelationRender.getInstance().renderList(props.current.children || [])}
            <Badge
                dot={relationValidate.isError()}
            >
                <Dropdown
                    menu={{items}}>
                    <Tag
                        style={{
                            cursor: "pointer",
                        }}
                    >）</Tag>
                </Dropdown>
            </Badge>
        </>
    )
}
import {Dropdown, Tag} from "antd";
import React from "react";
import {ConditionRelationProps} from "@/components/script/components/condition/typings";
import {
    useDropdownMenus
} from "@/components/script/components/condition/components/condition-relation/hooks/use-dropdown-menus";
import {RelationRender} from "@/components/script/components/condition/components/condition-relation/render";

export const RelationGroup: React.FC<ConditionRelationProps> = (props) => {
    const items = useDropdownMenus(props);

    return (
        <>
            <Dropdown
                menu={{items}}>
                <Tag
                    style={{
                        cursor: "pointer",
                    }}
                >（</Tag>
            </Dropdown>
            {RelationRender.getInstance().renderList(props.current.children || [])}
            <Dropdown
                menu={{items}}>
                <Tag
                    style={{
                        cursor: "pointer",
                    }}
                >）</Tag>
            </Dropdown>
        </>
    )
}
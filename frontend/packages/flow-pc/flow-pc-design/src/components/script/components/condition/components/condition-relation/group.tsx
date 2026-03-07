import {Dropdown, Tag} from "antd";
import React from "react";
import {ConditionRelationProps, LogicalRelation} from "@/components/script/components/condition/typings";
import {
    useDropdownMenus
} from "@/components/script/components/condition/components/condition-relation/hooks/use-dropdown-menus";
import {RelationRender} from "@/components/script/components/condition/components/condition-relation/render";

export const RelationGroup: React.FC<ConditionRelationProps> = (props) => {
    const items = useDropdownMenus(props);

    const relations = React.useMemo(() => {
        const list: LogicalRelation[] = [];
        if (props.current.children) {
            list.push(...props.current.children);
        }
        list.push({
            type: 'action'
        })
        return list;
    }, [props.current.children]);

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
            {RelationRender.getInstance().renderList(relations)}
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
import {PlusCircleOutlined} from "@ant-design/icons";
import React from "react";
import {ConditionRelationProps} from "@/components/script/components/condition/typings";
import { Dropdown } from "antd";
import {
    useDropdownMenus
} from "@/components/script/components/condition/components/condition-relation/hooks/use-dropdown-menus";

export const RelationAction: React.FC<ConditionRelationProps> = (props) => {
    const items = useDropdownMenus(props);

    return (
        <Dropdown
            menu={{items}}>
            <PlusCircleOutlined
                style={{
                    cursor: 'pointer',
                }}
            />
        </Dropdown>
    )
}
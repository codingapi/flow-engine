import {PlusCircleOutlined} from "@ant-design/icons";
import React from "react";
import {ConditionRelationProps} from "@/components/script/components/condition/typings";
import {Badge, Dropdown} from "antd";
import {
    useDropdownMenus
} from "@/components/script/components/condition/components/condition-relation/hooks/use-dropdown-menus";
import {
    RelationValidate
} from "@/components/script/components/condition/components/condition-relation/domain/relation-validate";

export const RelationAction: React.FC<ConditionRelationProps> = (props) => {
    const items = useDropdownMenus(props);
    const relationValidate = new RelationValidate(props);

    return (
        <Dropdown
            menu={{items}}>
            <Badge
                dot={relationValidate.isError()}
            >
                <PlusCircleOutlined
                    style={{
                        cursor: 'pointer',
                    }}
                />
            </Badge>
        </Dropdown>
    )
}
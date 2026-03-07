import React from "react";
import {useConditionContext} from "@/components/script/components/condition/hooks/use-condition-context";
import {ConditionRelationProps} from "@/components/script/components/condition/typings";
import {MenuRelation} from "@/components/script/components/condition/components/condition-relation/hooks/menu-relation";


export const useDropdownMenus = (props: ConditionRelationProps) => {
    const {state, context} = useConditionContext();
    const presenter = context.getPresenter().getConditionRelationPresenter();
    return React.useMemo(() => {
        const groups = state.groups;
        const menuRelation = new MenuRelation(presenter,props.current,groups,props.leftRelation);
        return menuRelation.getMenus();
    }, [state.groups]);
}
import {useConditionContext} from "@/components/script/components/condition/hooks/use-condition-context";
import React from "react";
import {RelationRender} from "@/components/script/components/condition/components/condition-relation/render";

export const RelationPanel = () => {

    const {state} = useConditionContext();

    const relations = React.useMemo(() => {
        const list = [...state.relations];
        list.push({
            type: 'action'
        })
        return list;
    }, [state.relations]);


    return (
        <div style={{
            padding: '4px 11px',
            border: '1px solid #d9d9d9',
            borderRadius: '6px',
            backgroundColor: '#fff',
            color: 'rgba(0,0,0,0.88)',
            minHeight: '60px',
            display: 'flex',        // 假设列表是flex布局
            flexWrap: 'wrap',       // 允许换行
            gap: '0 16px',          // 水平间距16px，垂直间距0
            alignItems: 'center',   // 垂直居中
        }}>
            {RelationRender.getInstance().renderList(relations)}
        </div>
    )
}
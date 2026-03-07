import React from "react";
import {ConditionRelationProps, LogicalRelation, LogicalType} from "@/components/script/components/condition/typings";
import {RelationOr} from "@/components/script/components/condition/components/condition-relation/or";
import {RelationAnd} from "@/components/script/components/condition/components/condition-relation/and";
import {RelationGroup} from "@/components/script/components/condition/components/condition-relation/group";
import {RelationAction} from "@/components/script/components/condition/components/condition-relation/action";
import {RelationCondition} from "@/components/script/components/condition/components/condition-relation/condition";


export class RelationRender {

    private relations: Map<LogicalType, React.ComponentType<ConditionRelationProps>>;

    private constructor() {
        this.relations = new Map();
        this.init();
    }

    private init() {
        this.relations.clear();
        this.relations.set("or", RelationOr);
        this.relations.set("and", RelationAnd);
        this.relations.set("group", RelationGroup);
        this.relations.set("action", RelationAction);
        this.relations.set("condition", RelationCondition);
    }

    private static readonly instance = new RelationRender();

    public static getInstance() {
        return this.instance;
    }

    public render(props: ConditionRelationProps) {
        const type = props.current.type;
        const RelationView = this.relations.get(type);
        if (RelationView) {
            return <RelationView {...props} />;
        }
    }

    public renderList(relations: LogicalRelation[]) {
        const list: React.ReactNode[] = [];
        const length = relations.length;
        for (let i = 0; i < length; i++) {
            const left = i - 1 >= 0 ? relations[i - 1] : undefined;
            const right = i + 1 > length ? undefined : relations[i + 1];
            const current = relations[i];
            list.push(this.render({
                leftRelation: left,
                current: current,
                rightRelation: right,
            }));
        }
        return list;
    }

}
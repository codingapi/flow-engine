import {ConditionRelationProps, LogicalRelation} from "@/components/script/components/condition/typings";

export class RelationValidate {

    private readonly leftRelation?: LogicalRelation;
    private readonly current: LogicalRelation;
    private readonly rightRelation?: LogicalRelation;


    constructor(props: ConditionRelationProps) {
        this.leftRelation = props.leftRelation;
        this.current = props.current;
        this.rightRelation = props.rightRelation;
    }

    public isError() {
        const currentType = this.current.type;
        const currentId = this.current.id;

        if (this.leftRelation) {
            const leftType = this.leftRelation.type;

            if (leftType === 'and' || leftType === 'or') {
                if (currentType === 'and' || currentType === 'or') {
                    return true;
                }
            }

            if (leftType === 'group' || leftType === 'condition') {
                if (currentType === 'group' || currentType === 'condition') {
                    return true;
                }
            }

        }
        if (this.rightRelation) {
            const rightType = this.rightRelation.type;
            if (rightType === 'action') {
                if (currentType === 'and' || currentType === 'or') {
                    return true;
                }
            }
            return false;
        } else {
            if (currentType === 'and' || currentType === 'or') {
                return true;
            }
        }
        if (currentType === 'action' && currentId) {
            return true;
        }
        return false;
    }
}


export class RelationListValidate {

    private readonly relations: LogicalRelation[];

    private error: boolean = false;

    constructor(relations: LogicalRelation[]) {
        this.relations = relations;
    }

    public isError() {
        this.fetchRelations(this.relations);
        return this.error;
    }

    private fetchRelations(relations: LogicalRelation[]) {
        const length = relations.length;
        for (let i = 0; i < length; i++) {
            const left = i - 1 < 0 ? undefined : relations[i - 1];
            const right = i + 1 > length ? undefined : relations[i + 1];
            const current = relations[i];

            const validate = new RelationValidate({
                leftRelation: left,
                current,
                rightRelation: right,
            });

            if (validate.isError()) {
                this.error = true;
            }
            if (current.children) {
                this.fetchRelations(current.children);
            }
        }

    }


}
import {
    Condition,
    ConditionGroup,
    ConditionState,
    LogicalRelation, RelationType
} from "@/components/script/components/condition/typings";
import {
    RelationListValidate
} from "@/components/script/components/condition/components/condition-relation/domain/relation-validate";
import {DataType} from "@flow-engine/flow-types";

class ConditionTitleConvertor {
    private readonly relations: LogicalRelation[];

    constructor(relations: LogicalRelation[]) {
        this.relations = relations;
    }

    public getTitle() {
        const builder = this.getLabel(this.relations);
        return builder.join(" ");
    }


    private getLabel(relations: LogicalRelation[]) {
        const builder: string[] = [];
        for (const relation of relations) {
            const relationType = relation.type;
            if (relationType !== "group") {
                if (relation.label) {
                    builder.push(relation.label.trim());
                }
            } else {
                if (relation.children) {
                    const children = this.getLabel(relation.children);
                    builder.push('(');
                    builder.push(...children);
                    builder.push(')');
                }
            }
        }
        return builder;
    }
}


class ConditionReturnConvertor {
    private readonly relations: LogicalRelation[];
    private readonly groups: ConditionGroup[];

    constructor(state: ConditionState) {
        this.relations = state.relations;
        this.groups = state.groups;
    }

    public getReturn() {
        const builder:string[] = this.getReturnExpression(this.relations);
        return builder.join(" ");
    }

    private getConditionGroup(id: string) {
        for (let group of this.groups) {
            if (group.id === id) {
                return group;
            }
        }
        return null;
    }

    private getConditionGroupDataType(conditionGroup: ConditionGroup): DataType {
        if (conditionGroup.left) {
            if (conditionGroup.left.type === 'variable') {
                return conditionGroup.left.dataType;
            }
        }
        if (conditionGroup.right) {
            if (conditionGroup.right.type === 'variable') {
                return conditionGroup.right.dataType;
            }
        }
        return 'STRING'
    }

    private getConditionExpression(condition:Condition,dataType:DataType){
        if(condition.type==='variable') {
            return condition.value;
        }
        if(dataType==='BOOLEAN'){
            return condition.value==='true';
        }

        if(dataType==='STRING'){
            return `'${condition.value}'`;
        }

        return `${condition.value}`;
    }

    private getConditionTypeExpression(relationType:RelationType){
        if(relationType==='equal') {
            return '==';
        }
        if(relationType==='not_equal') {
            return '!=';
        }
        if(relationType==='greater_equal') {
            return '>=';
        }
        if(relationType==='less_equal') {
            return '<=';
        }
        if(relationType==='greater_than') {
            return '>';
        }
        if(relationType==='less_than') {
            return '<';
        }
        return `${relationType}`;

    }


    private getConditionGroupExpression(id: string) {
        const conditionGroup = this.getConditionGroup(id);
        if (conditionGroup) {
            const dataType = this.getConditionGroupDataType(conditionGroup);
            const left = conditionGroup.left;
            const right = conditionGroup.right;
            const type = conditionGroup.type;

            if(left && right && type){
                const leftExpression = this.getConditionExpression(left,dataType);
                const rightExpression = this.getConditionExpression(right,dataType);
                const typeExpression = this.getConditionTypeExpression(type);
                return leftExpression + typeExpression + rightExpression;
            }
        }
        return '';
    }


    private getReturnExpression(relations: LogicalRelation[]) {
        const builder: string[] = [];

        for (const relation of relations) {
            const relationType = relation.type;
            if (relationType !== "group") {
                if (relationType === 'and') {
                    builder.push('&&');
                }
                if (relationType === 'or') {
                    builder.push('||');
                }
                if (relationType === 'condition') {
                    if (relation.groupId) {
                        builder.push(this.getConditionGroupExpression(relation.groupId));
                    }
                }
            } else {
                if (relation.children) {
                    builder.push('(');
                    const children = this.getReturnExpression(relation.children);
                    builder.push(...children);
                    builder.push(')');
                }
            }
        }

        return builder;
    }


}

export class ConditionGroovyConvertor {
    private readonly state: ConditionState;

    constructor(state: ConditionState) {
        this.state = state;
    }

    public isError() {
        const relationValidate = new RelationListValidate(this.state.relations);
        return relationValidate.isError();
    }

    private getMeta() {
        return JSON.stringify(this.state);
    }

    private getTitle() {
        const titleConvertor = new ConditionTitleConvertor(this.state.relations);
        return titleConvertor.getTitle();
    }

    private getReturn() {
        const returnConvertor = new ConditionReturnConvertor(this.state);
        return returnConvertor.getReturn();
    }


    public getScript() {
        return `// @SCRIPT_TITLE ${this.getTitle()}
        // @SCRIPT_META ${this.getMeta()}
        def run(request){
            return ${this.getReturn()};
        }
        `;
    }

}
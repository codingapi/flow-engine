import {ConditionGroup, relationTypeOptions} from "@/components/script/components/condition/typings";

export class GroupConvertor {

    private readonly group: ConditionGroup;

    constructor(group: ConditionGroup) {
        this.group = group;
    }

    public getLabel() {
        const left = this.group.left;
        const right = this.group.right;
        const type = this.group.type;
        if (left && right && type) {
            const relation = relationTypeOptions.find(relationTypeOption => relationTypeOption.value === type);
            return `${left.label} ${relation?.label} ${right.label}`;
        }
        return '未知';
    }
}
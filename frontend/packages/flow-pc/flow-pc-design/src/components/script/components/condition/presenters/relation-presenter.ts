import {Presenter} from "./index";
import {LogicalRelation} from "@/components/script/components/condition/typings";

/**
 *  条件关系的Presenter对象
 */
export class ConditionRelationPresenter {
    private readonly presenter: Presenter;

    constructor(presenter: Presenter) {
        this.presenter = presenter;
    }

    public removeRelation(id: string) {
        this.presenter.updateState(prevState => {
            return {
                ...prevState,
                relations: [...this.removeRelations(id,prevState.relations)]
            }
        })
    }

    public addRelation(target: LogicalRelation, sourceId?: string) {
        this.presenter.updateState(prevState => {
            if (sourceId) {
                return {
                    ...prevState,
                    relations: [...this.appendRelations(sourceId, prevState.relations, target)]
                }
            } else {
                return {
                    ...prevState,
                    relations: [...prevState.relations, target]
                }
            }
        })
    }

    private removeRelations(sourceId: string, relations: LogicalRelation[]) {
        if (relations.length === 0) {
            return [];
        }
        const list: LogicalRelation[] = [];
        for (const relation of relations) {
            let current = {
                ...relation,
            }
            if (relation.children) {
                current = {
                    ...relation,
                    children: this.removeRelations(sourceId, relation.children)
                }
            }
            if (relation.id !== sourceId) {
                list.push(current);
            }
        }
        return list;
    }

    private appendRelations(sourceId: string, relations: LogicalRelation[], target: LogicalRelation) {
        if (relations.length == 0) {
            return [target];
        }
        const list: LogicalRelation[] = [];
        for (const relation of relations) {
            let current = {
                ...relation
            }
            if (relation.children) {
                current = {
                    ...relation,
                    children:this.appendRelations(sourceId, relation.children, target)
                }
            }
            list.push(current);
            if (relation.id === sourceId) {
                list.push(target);
            }
        }
        return list;
    }

}
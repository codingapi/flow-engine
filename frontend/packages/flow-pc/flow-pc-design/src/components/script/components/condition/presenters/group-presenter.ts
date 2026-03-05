import {IdUtils} from "@/utils";
import {Presenter} from "./index";
import {ConditionGroup} from "@/components/script/components/condition/typings";

/**
 *  条件分组的Presenter对象
 */
export class ConditionGroupPresenter {
    private readonly presenter: Presenter;

    constructor(presenter: Presenter) {
        this.presenter = presenter;
    }

    /**
     *  添加条件
     */
    public addCondition() {
        this.presenter.updateState(prevState => {
            const groups = prevState.groups;
            const group: ConditionGroup = {
                id: IdUtils.generateId()
            }
            return {
                ...prevState,
                groups: [...groups, group]
            }
        })
    }


    /**
     * 删除条件
     * @param id
     */
    public removeCondition(id: string) {
        this.presenter.updateState(prevState => {
            const groups = prevState.groups.filter(group => group.id !== id);
            return {
                ...prevState,
                groups: groups
            }
        })
    }

    /**
     * 互换条件
     * @param id
     */
    public switchCondition(id: string) {
        this.presenter.updateState(prevState => {
            const target = prevState.groups.find(group => group.id === id);
            const groups = prevState.groups;
            const latest = [];
            if (target) {
                for (const group of groups) {
                    if (group.id === id) {
                        latest.push({
                            ...target,
                            left: target.right,
                            right: target.left
                        });
                    } else {
                        latest.push(group);
                    }
                }
            } else {
                latest.push(...groups);
            }
            return {
                ...prevState,
                groups: latest
            }
        })
    }


}
import {ConditionGroup, LogicalRelation} from "@/components/script/components/condition/typings";
import React from "react";
import {ConditionRelationPresenter} from "@/components/script/components/condition/presenters/relation-presenter";
import {GroupConvertor} from "@/components/script/components/condition/convertor/group";
import {IdUtils} from "@/utils";

export class MenuRelation {

    private readonly leftRelation?: LogicalRelation;
    private readonly current: LogicalRelation;
    private readonly menuList: any[];
    private readonly groups: ConditionGroup[];
    private readonly presenter: ConditionRelationPresenter;

    constructor(presenter: ConditionRelationPresenter,
                current: LogicalRelation,
                groups: ConditionGroup[],
                leftRelation?: LogicalRelation) {
        this.presenter = presenter;
        this.leftRelation = leftRelation;
        this.current = current;
        this.groups = groups;
        this.menuList = [];
        this.initMenus();
    }

    private initMenus() {
        const currentType = this.current.type;
        if (currentType === 'action') {
            if (this.leftRelation) {
                const leftType = this.leftRelation.type;
                if (leftType === 'and' || leftType === 'or') {
                    this.menuList.push(this.createGroup());
                    if (this.groups.length > 0) {
                        this.menuList.push(this.createCondition());
                    }
                }
                if (leftType === 'group' || leftType === 'condition') {
                    this.menuList.push(this.createAnd());
                    this.menuList.push(this.createOr());
                }
            } else {
                this.menuList.push(this.createGroup());
                if (this.groups.length > 0) {
                    this.menuList.push(this.createCondition());
                }
            }
        }

        if(currentType==='and' || currentType==='or') {
            this.menuList.push(this.createGroup());
            if (this.groups.length > 0) {
                this.menuList.push(this.createCondition());
            }
        }

        if(currentType==='group' || currentType==='condition') {
            this.menuList.push(this.createAnd());
            this.menuList.push(this.createOr());
        }

        if(this.current.id) {
            this.menuList.push(this.createRemove());
        }
    }


    private createOr() {
        return {
            key: 'or',
            label: (
                <a>
                    或者
                </a>
            ),
            onClick: () => {
                this.addRelation({
                    type: 'or',
                    id: IdUtils.generateId(),
                    label: '或者'
                });
            }
        }
    }

    private createCondition() {
        const children = this.groups.map(item => {
            const convert = new GroupConvertor(item)
            return {
                label: convert.getLabel(),
                value: item.id,
                key: item.id,
                onClick: () => {
                    this.addRelation({
                        id: IdUtils.generateId(),
                        type: 'condition',
                        label: convert.getLabel(),
                        groupId: item.id
                    });
                }
            }
        });

        return {
            key: 'condition',
            label: (
                <a>
                    条件
                </a>
            ),
            children: children
        }
    }

    private createRemove() {
        return {
            key: 'remove',
            danger: true,
            label: (
                <a>
                    删除
                </a>
            ),
            onClick: () => {
                this.removeRelation();
            }
        }
    }

    private createGroup() {
        return {
            key: 'group',
            label: (
                <a>
                    括号
                </a>
            ),
            onClick: () => {
                this.addRelation({
                    type: 'group',
                    id: IdUtils.generateId(),
                    label: '括号',
                    children: [
                        {
                            id: IdUtils.generateId(),
                            type: 'action'
                        }
                    ]
                });
            }
        }
    }

    private createAnd() {
        return {
            key: 'and',
            label: (
                <a>
                    并且
                </a>
            ),
            onClick: () => {
                this.addRelation({
                    type: 'and',
                    id: IdUtils.generateId(),
                    label: '并且'
                });
            }
        }
    }

    private removeRelation = () => {
        const currentId = this.current.id;
        if (currentId) {
            this.presenter.removeRelation(currentId);
        }
    }

    private addRelation = (target: LogicalRelation) => {
        const currentId = this.current.id;
        this.presenter.addRelation(
            target,
            currentId
        );
    }

    public getMenus() {
        return this.menuList;
    }
}
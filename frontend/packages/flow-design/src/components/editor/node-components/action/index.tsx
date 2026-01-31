import {ActionType} from "@/components/editor/typings/node-type";
import React from "react";
import {AddAuditAction} from "@/components/editor/node-components/action/add-audit";
import {CustomAction} from "@/components/editor/node-components/action/custom";
import {DelegateAction} from "@/components/editor/node-components/action/delegate";
import {PassAction} from "@/components/editor/node-components/action/pass";
import {RejectAction} from "@/components/editor/node-components/action/reject";
import {ReturnAction} from "@/components/editor/node-components/action/return";
import {SaveAction} from "@/components/editor/node-components/action/save";
import {TransferAction} from "@/components/editor/node-components/action/transfer";

const actionMap: Map<ActionType, React.ComponentType> = new Map();
actionMap.set('ADD_AUDIT', AddAuditAction);
actionMap.set('CUSTOM', CustomAction);
actionMap.set('DELEGATE', DelegateAction);
actionMap.set('PASS', PassAction);
actionMap.set('REJECT', RejectAction);
actionMap.set('RETURN', ReturnAction);
actionMap.set('SAVE', SaveAction);
actionMap.set('TRANSFER', TransferAction);


export class ActionManager {

    private readonly actions: ActionType[] = [];

    public constructor(actions: ActionType[]) {
        this.actions = actions;
    }

    public getComponent(type: ActionType): React.ComponentType | undefined {
        for (const key in this.actions) {
            if (this.actions[key] === type) {
                return actionMap.get(type);
            }
        }
        return undefined;
    }

    public isEmpty(): boolean {
        return this.actions.length === 0;
    }

    public render(type: ActionType) {
        const Component = this.getComponent(type);
        if (Component) {
            return <Component/>;
        }
    }

}
import React from "react";
import {FlowActionProps} from "./type";
import {FlowAction} from "@flow-engine/flow-types";
import {PassAction} from "@/components/flow-approval/components/action/pass";
import {AddAuditAction} from "@/components/flow-approval/components/action/add-audit";
import {CustomAction} from "@/components/flow-approval/components/action/custom";
import {DelegateAction} from "@/components/flow-approval/components/action/delegate";
import {RejectAction} from "@/components/flow-approval/components/action/reject";
import {ReturnAction} from "@/components/flow-approval/components/action/return";
import {SaveAction} from "@/components/flow-approval/components/action/save";
import {TransferAction} from "@/components/flow-approval/components/action/transfer";

export class ActionFactory {

    private readonly cache: Map<string, React.ComponentType<FlowActionProps>>;

    private constructor() {
        this.cache = new Map();
        this.initActions();
    }

    private static readonly instance = new ActionFactory();

    public static getInstance() {
        return ActionFactory.instance;
    }


    private initActions() {
        this.cache.set("ADD_AUDIT", AddAuditAction);
        this.cache.set("CUSTOM", CustomAction);
        this.cache.set("DELEGATE", DelegateAction);
        this.cache.set("PASS", PassAction);
        this.cache.set("REJECT", RejectAction);
        this.cache.set("RETURN", ReturnAction);
        this.cache.set("SAVE", SaveAction);
        this.cache.set("TRANSFER", TransferAction);

    }

    public render(action: FlowAction) {
        const FlowActionComponent = this.cache.get(action.type);
        if (FlowActionComponent) {
            return (
                <FlowActionComponent action={action}/>
            )
        }
    }

}
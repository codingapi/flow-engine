import React from "react";
import {ActionFormProps} from "@/components/script/typings";
import {ActionType} from "@flow-engine/flow-types";
import {CustomActionForm} from "@/components/script/components/action/components/custom";
import {AddAuditActionForm} from "@/components/script/components/action/components/add-audit";
import {DelegateActionForm} from "@/components/script/components/action/components/delegate";
import {RejectActionForm} from "@/components/script/components/action/components/reject";
import {ReturnActionForm} from "@/components/script/components/action/components/return";
import {TransferActionForm} from "@/components/script/components/action/components/transfer";

export class ActionFactory{

    private readonly actions:Map<ActionType, React.ComponentType<ActionFormProps>>;

    private constructor() {
        this.actions = new Map();
        this.initialize();
    }

    private initialize():void{
        this.actions.set('ADD_AUDIT',AddAuditActionForm);
        this.actions.set('CUSTOM',CustomActionForm);
        this.actions.set('DELEGATE',DelegateActionForm);
        this.actions.set('REJECT',RejectActionForm);
        this.actions.set('RETURN',ReturnActionForm);
        this.actions.set('TRANSFER',TransferActionForm);

    }

    private static readonly instance = new ActionFactory();

    public static getInstance(){
        return this.instance;
    }

    public getActionForm(type:ActionType){
        return this.actions.get(type);
    }

}
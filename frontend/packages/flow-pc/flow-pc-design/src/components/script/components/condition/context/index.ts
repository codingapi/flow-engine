import React from "react";
import {Presenter} from "../presenters";
import {ConditionState, ConditionViewProps} from "../typings";

export class ConditionContextScope {
    private readonly presenter: Presenter;
    private readonly props: ConditionViewProps;

    constructor(presenter: Presenter, props: ConditionViewProps) {
        this.presenter = presenter;
        this.props = props;
    }

    public syncState(state: ConditionState) {
        this.presenter.syncState(state);
    }

    public getPresenter() {
        return this.presenter;
    }

    public initState(initData:any) {
        this.presenter.initState(initData);
    }

    public clearState(){
        this.presenter.clearState();
    }
}


export const ConditionContext = React.createContext<ConditionContextScope | undefined>(undefined);


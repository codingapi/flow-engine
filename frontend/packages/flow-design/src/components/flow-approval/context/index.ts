import React from "react";
import {ApprovalLayoutProps, State} from "../typings";
import {Presenter} from "../presenters";


export class ApprovalContextScope {
    private readonly presenter: Presenter;
    private readonly props: ApprovalLayoutProps;

    constructor(presenter: Presenter, props: ApprovalLayoutProps) {
        this.presenter = presenter;
        this.props = props;
    }

    public syncState(state: State) {
        this.presenter.syncState(state);
    }

    public getPresenter() {
        return this.presenter;
    }

    public close() {
        this.props.onClose?.();
    }

    public initialState() {
        this.presenter.initialState({
            flow: this.props.content,
        });
    }
}


export const ApprovalContext = React.createContext<ApprovalContextScope | undefined>(undefined);


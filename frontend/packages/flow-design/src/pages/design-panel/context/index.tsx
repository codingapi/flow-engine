import React from "react";
import {DesignPanelProps, State} from "../types";
import {Presenter} from "../presenters";


export class DesignPanelContextScope {
    private readonly presenter: Presenter;
    private readonly props: DesignPanelProps;

    constructor(presenter: Presenter, props: DesignPanelProps) {
        this.presenter = presenter;
        this.props = props;
    }

    public close() {
        this.presenter.initState();
        this.props.onClose?.();
    }

    public save() {
        this.presenter.save();
        // this.close();
    }

    public syncState(state: State) {
        this.presenter.syncState(state);
    }

    public getPresenter() {
        return this.presenter;
    }
}


export const DesignPanelContext = React.createContext<DesignPanelContextScope | undefined>(undefined);


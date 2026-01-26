import React from "react";
import {State} from "../types";
import {Presenter} from "../presenters";


export class DesignPanelContextScope {

    public state:State;
    private readonly presenter:Presenter;

    constructor(state:State,presenter:Presenter) {
        this.state = state;
        this.presenter = presenter;
    }

    public syncState(state:State) {
        this.state = state;
        this.presenter.syncState(state);
    }

    public getPresenter() {
        return this.presenter;
    }
}


export const DesignPanelContext = React.createContext<DesignPanelContextScope|undefined>(undefined);


import React from "react";
import {DesignPanelProps, State} from "../types";
import {Presenter} from "../presenters";


export class DesignPanelContextScope {

    public state:State;
    private readonly presenter:Presenter;
    private readonly props:DesignPanelProps;

    constructor(state:State,presenter:Presenter, props:DesignPanelProps) {
        this.state = state;
        this.presenter = presenter;
        this.props = props;
    }

    public close(){
        this.props.onClose?.();
    }

    public save(){
        console.log('save design ...')
        this.close();
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


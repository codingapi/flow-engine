import React from "react";
import {State} from "../types";
import {Presenter} from "../presenters";


export class DesignPanelContextScope {

    public readonly state:State;
    private readonly presenter:Presenter;

    constructor(state:State,presenter:Presenter) {
        this.presenter = presenter;
        this.state = state;
    }
}


export const DesignPanelContext = React.createContext<DesignPanelContextScope|undefined>(undefined);


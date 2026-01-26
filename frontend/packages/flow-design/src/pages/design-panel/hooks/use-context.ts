import React from "react";
import {PresenterHooks} from "@flow-engine/flow-core";
import {DesignPanelContext, DesignPanelContextScope} from "../context";
import {Presenter} from "../presenters";
import {DesignPanelApiImpl} from "../model";
import {initState} from "../types";

export const useContext = () => {
    const context = React.useContext(DesignPanelContext);
    if(!context){
        throw new Error("DesignPanelContext must be used within useContext");
    }
    return context;
}

export const createContext = () => {
    const ref = React.useRef<DesignPanelContextScope|undefined>();

    const {state, presenter} = PresenterHooks.create(Presenter, initState, new DesignPanelApiImpl());

    if(!ref.current) {
        ref.current = new DesignPanelContextScope(state,presenter);
    }
    return ref.current;
}
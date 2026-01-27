import React from "react"
import {ActionType} from "@/components/table";
import {PresenterHooks} from "@flow-engine/flow-core";
import {State} from "../types"
import {Presenter} from "../presenter";
import {DesignListApiImpl} from "../model";

const initState: State = {
    pageVersion: 0,
    editable: false,
    currentId:''
}

export const usePresenter = (actionType: React.RefObject<ActionType>) => {

    const {state, presenter} = PresenterHooks.create(Presenter, initState, new DesignListApiImpl());

    React.useEffect(() => {
        if (state.pageVersion) {
            actionType.current?.reload();
        }
    }, [state.pageVersion]);

    return {
        state,
        presenter
    }
}
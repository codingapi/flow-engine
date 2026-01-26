import React from "react"
import {State} from "../types"
import {Presenter} from "../presenter";
import {DesignListApiImpl} from "../model";
import {ActionType} from "@/components/table";


const initStata: State = {
    pageVersion: 0,
    editable: false
}

export const usePresenter = (actionType: React.RefObject<ActionType>) => {

    const [state, dispatch] = React.useState<State>(initStata);

    const ref = React.useRef<Presenter | null>(null);


    if (!ref.current) {
        ref.current = new Presenter(state, dispatch, new DesignListApiImpl());
    }

    React.useEffect(() => {
        ref.current?.syncState(state);
    }, [state]);

    React.useEffect(() => {
        if (state.pageVersion) {
            actionType.current?.reload();
        }
    }, [state.pageVersion]);


    return {
        state,
        presenter: ref.current
    }
}
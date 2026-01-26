import React from "react"
import { State } from "../types"
import { Presenter } from "../presenter";
import { DesignListApiImpl } from "../model";


const initStata:State = {
    
}

export const usePresenter = () => {

    const [state, dispatch] = React.useState<State>(initStata);

    const ref = React.useRef<Presenter | null>(null);


    if (!ref.current) {
        ref.current = new Presenter(state, dispatch,new DesignListApiImpl());
    }

    React.useEffect(() => {
        ref.current?.syncState(state);
    }, [state]);


    return {
        state,
        presenter:ref.current
    }
}
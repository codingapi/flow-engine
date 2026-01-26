import React from "react";
import {BasePresenter, PresenterConstructor} from "@/presenter.ts";

export class PresenterHooks {

    public static create<P extends BasePresenter<S, M>,S,M>(PresenterClass:PresenterConstructor<P,S,M>,
                                                            initStata:S,
                                                            model:M){
        const [state, dispatch] = React.useState<S>(initStata);

        const ref = React.useRef<P | null>(null);

        if (!ref.current) {
            ref.current = new PresenterClass(state, dispatch, model);
        }

        React.useEffect(() => {
            ref.current?.syncState(state);
        }, [state]);

        return {
            state,
            presenter: ref.current
        }
    }

}
import React from "react";
import {useDispatch, useSelector} from "react-redux";
import {DesignPanelContext, DesignPanelContextScope} from "../context";
import {DesignReduxState, updateState} from "../store";
import {Presenter} from "../presenters";
import {DesignPanelApiImpl} from "../model";
import {DesignPanelProps} from "../types";

export const useContext = () => {
    const context = React.useContext(DesignPanelContext);
    const state = useSelector((state: DesignReduxState) => state.design);
    if (!context) {
        throw new Error("DesignPanelContext must be used within useContext");
    }
    return {
        state,
        context,
    };
}

export const createContext = (props:DesignPanelProps) => {
    const ref = React.useRef<DesignPanelContextScope | undefined>();

    const dispatch = useDispatch();

    const state = useSelector((state: DesignReduxState) => state.design);

    if (!ref.current) {
        const presenter = new Presenter(
            state,
            (preState ) => {
                dispatch(updateState({
                    ...preState,
                }));
                return preState;
            },
            new DesignPanelApiImpl()
        );
        ref.current = new DesignPanelContextScope(state, presenter,props);
    }

    React.useEffect(() => {
        ref.current?.syncState(state);
    }, [state]);

    return {
        state,
        context: ref.current,
    }
}
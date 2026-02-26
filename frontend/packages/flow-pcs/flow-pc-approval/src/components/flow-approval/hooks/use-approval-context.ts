import React from "react";
import {useDispatch, useSelector} from "react-redux";
import {ApprovalContext, ApprovalContextScope} from "../context";
import {ApprovalReduxState, updateState} from "../store";
import {Presenter} from "../presenters";
import {FlowApprovalApiImpl} from "../model";
import {ApprovalLayoutProps} from "@/components/flow-approval/typings";

export const useApprovalContext = () => {
    const context = React.useContext(ApprovalContext);
    const state = useSelector((state: ApprovalReduxState) => state.approval);
    if (!context) {
        throw new Error("ApprovalContext must be used within useContext");
    }
    return {
        state,
        context,
    };
}

export const createApprovalContext = (props: ApprovalLayoutProps) => {
    const ref = React.useRef<ApprovalContextScope | undefined>(undefined);

    const dispatch = useDispatch();

    const state = useSelector((state: ApprovalReduxState) => state.approval);

    if (!ref.current) {
        const presenter = new Presenter(
            state,
            (prevState) => {
                dispatch(updateState(prevState));
                return prevState;
            },
            new FlowApprovalApiImpl()
        );
        ref.current = new ApprovalContextScope(presenter, props);
        ref.current.initialState();
    }

    React.useEffect(() => {
        if(state.flow) {
            ref.current?.syncState(state);
        }
    }, [state]);

    return {
        state,
        context: ref.current,
    }
}
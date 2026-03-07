import React from "react";
import {Presenter} from "../presenters";
import {useDispatch, useSelector} from "react-redux";
import {ConditionContext, ConditionContextScope} from "../context";
import {ConditionReduxState, updateState} from "../store";
import {ConditionApiImpl} from "../models";
import {ConditionViewProps} from "../typings";
import {useScriptMetaData} from "@/components/script/hooks/use-script-meta-data";

export const useConditionContext = () => {
    const context = React.useContext(ConditionContext);
    const state = useSelector((state: ConditionReduxState) => state.condition);
    if (!context) {
        throw new Error("ConditionContext must be used within useContext");
    }
    return {
        state,
        context,
    };
}

export const createConditionContext = (props: ConditionViewProps) => {
    const ref = React.useRef<ConditionContextScope | undefined>(undefined);

    const dispatch = useDispatch();

    const state = useSelector((state: ConditionReduxState) => state.condition);

    const initData = useScriptMetaData(props.script);

    if (!ref.current) {
        const presenter = new Presenter(
            state,
            (prevState) => {
                dispatch(updateState(prevState));
                return prevState;
            },
            new ConditionApiImpl()
        );
        ref.current = new ConditionContextScope(presenter, props);
        ref.current.initState({
            ...initData,
            variables: props.variables
        });
    }

    React.useEffect(() => {
        ref.current?.syncState(state);
    }, [state]);


    React.useEffect(() => {
        // 关闭时清空redux的数据
        return () => {
            ref.current?.clearState();
            ref.current = undefined;
        }
    }, []);

    return {
        state,
        context: ref.current,
    }
}
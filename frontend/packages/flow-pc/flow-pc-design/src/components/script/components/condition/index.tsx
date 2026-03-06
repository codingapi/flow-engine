import React from "react";
import {ConditionPanel} from "./components/condition-panel";
import {Provider} from "react-redux";
import {conditionStore} from "./store";
import {ConditionViewProps} from "./typings";
import {createConditionContext} from "./hooks/use-condition-context";
import {ConditionContext} from "./context";


const ConditionViewScope: React.FC<ConditionViewProps> = (props) => {
    const {context} = createConditionContext(props);
    return (
        <ConditionContext.Provider value={context}>
            <ConditionPanel {...props}/>
        </ConditionContext.Provider>
    )
}

export const ConditionView: React.FC<ConditionViewProps> = (props) => {
    return (
        <Provider store={conditionStore}>
            <ConditionViewScope {...props}/>
        </Provider>
    )
}
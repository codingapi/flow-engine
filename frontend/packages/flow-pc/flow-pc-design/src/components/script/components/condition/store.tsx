import {configureStore, createSlice, PayloadAction} from '@reduxjs/toolkit';
import {initStateData, ConditionState} from "./typings";
import { original } from 'immer';

export type ConditionStoreAction = {
    updateState: (state: ConditionState, action: PayloadAction<Partial<ConditionState> | ((prev: ConditionState) => Partial<ConditionState>)>) => void;
}

export const conditionSlice = createSlice<ConditionState, ConditionStoreAction, "condition", {}>({
    name: 'condition',
    initialState: {
        ...initStateData
    },
    reducers: {
        updateState: (state, action) => {
            if(typeof action.payload === 'function') {
                const currentState = original(state) as ConditionState;
                Object.assign(state, action.payload(currentState));
            }else {
                Object.assign(state, action.payload);
            }
        },
    },
});


export const {
    updateState,
} = conditionSlice.actions;
export const conditionStore = configureStore({
    reducer: {
        condition: conditionSlice.reducer
    },
});

export type ConditionReduxState = ReturnType<typeof conditionStore.getState>;
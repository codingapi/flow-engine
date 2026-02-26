import {configureStore, createSlice, PayloadAction} from '@reduxjs/toolkit';
import {initStateData, State} from "./typings";
import { original } from 'immer';

export type ApprovalStoreAction = {
    updateState: (state: State, action: PayloadAction<Partial<State> | ((prev: State) => Partial<State>)>) => void;
}

export const approvalSlice = createSlice<State, ApprovalStoreAction, "approval", {}>({
    name: 'approval',
    initialState: {
        ...initStateData
    },
    reducers: {
        updateState: (state, action) => {
            if(typeof action.payload === 'function') {
                const currentState = original(state) as State;
                Object.assign(state, action.payload(currentState));
            }else {
                Object.assign(state, action.payload);
            }
        },
    },
});


export const {
    updateState,
} = approvalSlice.actions;
export const approvalStore = configureStore({
    reducer: {
        approval: approvalSlice.reducer
    },
});

export type ApprovalReduxState = ReturnType<typeof approvalStore.getState>;
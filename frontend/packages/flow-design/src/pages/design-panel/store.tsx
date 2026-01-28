import {configureStore, createSlice, PayloadAction} from '@reduxjs/toolkit';
import {initStateData, State} from "./types";
import { original } from 'immer';

export type DesignStoreAction = {
    updateState: (state: State, action: PayloadAction<Partial<State> | ((prev: State) => Partial<State>)>) => void;
}

export const designSlice = createSlice<State, DesignStoreAction, "design", {}>({
    name: 'design',
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
} = designSlice.actions;
export const designStore = configureStore({
    reducer: {
        design: designSlice.reducer
    },
});

export type DesignReduxState = ReturnType<typeof designStore.getState>;
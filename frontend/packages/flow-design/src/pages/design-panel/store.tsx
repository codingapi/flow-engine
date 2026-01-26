import {configureStore, createSlice, PayloadAction} from '@reduxjs/toolkit';
import {initStateData, State} from "./types";


export type DesignStoreAction = {
    updateState: (state: State, action: PayloadAction<Partial<State>>) => void;
}

export const designSlice = createSlice<State, DesignStoreAction, "design", {}>({
    name: 'design',
    initialState: {
        ...initStateData
    },
    reducers: {
        updateState: (state, action) => {
            Object.assign(state, action.payload);
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
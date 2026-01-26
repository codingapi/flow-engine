import {DesignPanelApi, PanelTabType, State} from "../types";

export class Presenter {

    private state: State;
    private readonly dispatch: (state: State) => void;
    private readonly api: DesignPanelApi;

    constructor(state: State,dispatch: (state: State) => void, api: DesignPanelApi) {
        this.api = api;
        this.dispatch = dispatch;
        this.state = state;
    }

    public syncState(state: State) {
        this.state = state;
    }

    switchPanelTab(tab:PanelTabType){
        this.dispatch({
            ...this.state,
            panelTab: tab
        })
    }
}
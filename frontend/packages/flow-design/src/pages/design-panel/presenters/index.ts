import {DesignPanelApi, TabPanelType, State} from "../types";

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

    public updateViewPanelTab(tab:TabPanelType){
        this.dispatch({
            ...this.state,
            view:{
                ...this.state.view,
                tabPanel: tab,
            }
        })
    }

    public updateWorkflow(data:any) {
        this.dispatch({
            ...this.state,
            workflow:{
                ...this.state.workflow,
                ...data
            }
        });
    }


}
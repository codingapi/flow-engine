import {type Dispatch} from "@flow-engine/flow-core";
import {DesignListApi, State} from "./types";
import {ParamRequest} from "../table";


export class Presenter {

    private readonly api: DesignListApi;
    private readonly dispatch: Dispatch<State>;
    private state: State;

    public constructor(state: State, dispatch: Dispatch<State>, api: DesignListApi) {
        this.api = api;
        this.dispatch = dispatch;
        this.state = state;
    }

    public syncState(state: State) {
        this.state = state;
    }


    public request(request: ParamRequest) {
        return this.api.request(request);
    }


    public reload() {
        this.dispatch(preState => {
            return {
                ...preState,
                pageVersion: this.state.pageVersion + 1,
            }
        })
    }

    public closeEditable() {
        this.dispatch(preState => {
            return {
                ...preState,
                editable: false
            }
        })
    }

    public showEditable() {
        this.dispatch(preState => {
            return {
                ...preState,
                editable: true
            }
        })
    }




}
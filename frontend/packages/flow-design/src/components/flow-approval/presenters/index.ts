import {FlowApprovalApi, State} from "@/components/flow-approval/typings";
import {Dispatch} from "@flow-engine/flow-core";

export class Presenter {

    private state: State;
    private readonly dispatch: Dispatch<State>;
    private readonly api: FlowApprovalApi;

    constructor(state: State, dispatch: Dispatch<State>, api: FlowApprovalApi) {
        this.state = state;
        this.dispatch = dispatch;
        this.api = api;
    }

    public syncState(state: State) {
        this.state = state;
    }

    public initialState(state: State) {
        this.dispatch(state);
    }

}
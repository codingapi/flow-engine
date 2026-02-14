import {FlowApprovalApi, State} from "@/components/flow-approval/typings";
import {Dispatch} from "@flow-engine/flow-core";
import {FormActionContext} from "@/components/flow-approval/presenters/form";

export class Presenter {

    private state: State;
    private readonly dispatch: Dispatch<State>;
    private readonly api: FlowApprovalApi;
    private readonly formActionContext:FormActionContext;

    constructor(state: State, dispatch: Dispatch<State>, api: FlowApprovalApi) {
        this.state = state;
        this.dispatch = dispatch;
        this.api = api;
        this.formActionContext = new FormActionContext();
    }

    public syncState(state: State) {
        this.state = state;
    }


    public getFormActionContext() {
        return this.formActionContext;
    }


    public initialState(state: State) {
        this.dispatch(state);
    }

}
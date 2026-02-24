import {FlowApprovalApi, State} from "@/components/flow-approval/typings";
import {Dispatch} from "@flow-engine/flow-core";
import {FormActionContext} from "@/components/flow-approval/presenters/form";
import {FlowActionPresenter} from "@/components/flow-approval/presenters/action";

export class Presenter {

    private state: State;
    private readonly dispatch: Dispatch<State>;
    private readonly api: FlowApprovalApi;
    private readonly formActionContext:FormActionContext;
    private readonly flowActionPresenter:FlowActionPresenter;

    constructor(state: State, dispatch: Dispatch<State>, api: FlowApprovalApi) {
        this.state = state;
        this.dispatch = dispatch;
        this.api = api;
        this.formActionContext = new FormActionContext();
        this.flowActionPresenter = new FlowActionPresenter(state,api,this.formActionContext);
    }

    public syncState(state: State) {
        this.state = state;
        this.flowActionPresenter.syncState(state);
    }


    public getFormActionContext() {
        return this.formActionContext;
    }

    public getFlowActionPresenter() {
        return this.flowActionPresenter;
    }


    public initialState(state: State) {
        this.dispatch(state);
    }


    public processNodes(){
        return this.flowActionPresenter.processNodes();
    }

}
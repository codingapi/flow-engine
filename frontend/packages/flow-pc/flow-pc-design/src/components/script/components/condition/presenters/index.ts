import {Dispatch} from "@flow-engine/flow-core";
import {ConditionApi, ConditionState} from "../typings";

export class Presenter {

    private state: ConditionState;
    private readonly dispatch: Dispatch<ConditionState>;
    private readonly api: ConditionApi;

    constructor(state: ConditionState, dispatch: Dispatch<ConditionState>, api: ConditionApi) {
        this.api = api;
        this.dispatch = dispatch;
        this.state = state;
    }

    public syncState(state: ConditionState) {
        this.state = state;
    }

    public initState(initData: any) {
        this.dispatch(initData);
    }
}
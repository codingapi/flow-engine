import { type Dispatch } from "@flow-engine/flow-core";
import { DesignListApi, State } from "./types";
import { ParamRequest } from "../table";


export class Presenter {

    private readonly api: DesignListApi;
    private readonly dispatch: Dispatch<State>;
    private state: State;

    public constructor(state: State, dispath: Dispatch<State>, api: DesignListApi) {
        this.api = api;
        this.dispatch = dispath;
        this.state = state;
    }

    public syncState(state: State) {
        this.state = state;
    }


    public request(request: ParamRequest) {
        return this.api.request(request);
    }


}
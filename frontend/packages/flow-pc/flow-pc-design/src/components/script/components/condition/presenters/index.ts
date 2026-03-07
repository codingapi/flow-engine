import {Dispatch} from "@flow-engine/flow-core";
import {ConditionApi, ConditionState, initStateData} from "../typings";
import {ConditionGroupPresenter} from "./group-presenter";
import {ConditionRelationPresenter} from "@/components/script/components/condition/presenters/relation-presenter";

/**
 *  条件总控的Presenter
 */
export class Presenter {

    private state: ConditionState;
    private readonly dispatch: Dispatch<ConditionState>;
    private readonly api: ConditionApi;

    private readonly groupPresenter: ConditionGroupPresenter;
    private readonly relationPresenter: ConditionRelationPresenter;

    constructor(state: ConditionState, dispatch: Dispatch<ConditionState>, api: ConditionApi) {
        this.api = api;
        this.dispatch = dispatch;
        this.state = state;
        this.groupPresenter = new ConditionGroupPresenter(this);
        this.relationPresenter = new ConditionRelationPresenter(this);
    }

    public getConditionGroupPresenter() {
        return this.groupPresenter;
    }

    public getConditionRelationPresenter() {
        return this.relationPresenter;
    }

    public syncState(state: ConditionState) {
        this.state = state;
    }

    public initState(initData: any) {
        this.dispatch(initData);
    }

    public clearState() {
        this.dispatch(initStateData);
    }

    public updateState(state: ((prevState: ConditionState) => ConditionState) | ConditionState) {
        this.dispatch(state);
    }

}

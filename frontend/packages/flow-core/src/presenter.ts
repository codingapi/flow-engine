import {Dispatch} from "@/dispatch.ts";

export class BasePresenter<S,M> {

    protected state:S;
    protected readonly dispatch:Dispatch<S>;
    protected readonly model:M;

    public constructor(state:S, dispatch:Dispatch<S>,model:M){
        this.state = state;
        this.dispatch = dispatch;
        this.model = model;
    }

    public syncState(state:S){
        this.state = state;
    }
}

export type PresenterConstructor<P extends BasePresenter<S, M>, S, M> = {
    new(state: S, dispatch: Dispatch<S>, model: M): P;
};

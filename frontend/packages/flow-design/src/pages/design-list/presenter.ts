import {DesignListApi, State} from "./types";
import {ParamRequest} from "@/components/table";
import {BasePresenter} from "@flow-engine/flow-core";

export class Presenter extends BasePresenter<State, DesignListApi> {

    public request(request: ParamRequest) {
        return this.model.request(request);
    }

    public reload() {
        this.dispatch(preState => {
            return {
                ...preState,
                pageVersion: this.state.pageVersion + 1,
            }
        })
    }

    public hideEditable() {
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
import {DesignListApi, State} from "./types";
import {ParamRequest} from "@flow-engine/flow-core";
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

    public async deleteRecord(id: string) {
        await this.model.delete(id).then(result => {
            this.reload();
        });
    }

    public hideEditable() {
        this.dispatch(preState => {
            return {
                ...preState,
                editable: false
            }
        })
    }

    public async changeState(id:string){
        await this.model.changeState(id).then(result => {
            this.reload();
        });
    }

    public showEditable() {
        this.dispatch(preState => {
            return {
                ...preState,
                editable: true
            }
        })
    }

    public clearCurrent() {
        this.dispatch(preState => {
            return {
                ...preState,
                currentId: '',
            }
        })
    }

    public editCurrent(id: string) {
        this.dispatch(preState => {
            return {
                ...preState,
                currentId: id,
                editable: true
            }
        })
    }



}
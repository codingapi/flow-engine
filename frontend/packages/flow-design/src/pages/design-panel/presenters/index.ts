import {BasePresenter} from "@flow-engine/flow-core";
import {DesignPanelApi, PanelTabType, State} from "../types";

export class Presenter extends BasePresenter<State, DesignPanelApi>{

    close() {
    }


    save(){

    }

    switchPanelTab(tab:PanelTabType){
        this.dispatch(preState=>{
            return {
                ...preState,
                panelTab: tab
            }
        })
    }
}
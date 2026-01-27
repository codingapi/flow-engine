import {DesignPanelApi, initStateData, State, TabPanelType} from "../types";
import {Dispatch} from "@flow-engine/flow-core";
import {FormActionContext} from "@/pages/design-panel/presenters/form";

export class Presenter {

    private state: State;
    private readonly dispatch: Dispatch<State>;
    private readonly api: DesignPanelApi;
    private readonly formActionContext: FormActionContext;

    constructor(state: State, dispatch: Dispatch<State>, api: DesignPanelApi) {
        this.api = api;
        this.dispatch = dispatch;
        this.state = state;
        this.formActionContext = new FormActionContext();
    }


    public getFormActionContext() {
        return this.formActionContext;
    }

    public syncState(state: State) {
        this.state = state;
    }

    public updateViewPanelTab(tab: TabPanelType) {
        const values = this.formActionContext.save();
        this.dispatch((prevState: State) => {
            return {
                ...prevState,
                view: {
                    ...prevState.view,
                    tabPanel: tab,
                },
                workflow: {
                    ...prevState.workflow,
                    ...values
                }
            }
        });
    }

    public save() {
        const values = this.formActionContext.save();
        this.updateWorkflow(values);
        const latest = {
            ...this.state,
            workflow: {
                ...this.state.workflow,
                ...values
            }
        };
        console.log('save latest:', latest);
    }

    public updateWorkflow(data: any) {
        this.dispatch((prevState: State) => {
            return {
                ...prevState,
                workflow: {
                    ...prevState.workflow,
                    ...data
                }
            }
        });
    }


    public initState() {
        this.dispatch(initStateData);
    }
}
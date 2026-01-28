import {DesignPanelApi, initStateData, State, TabPanelType} from "../types";
import {Dispatch} from "@flow-engine/flow-core";
import {FormActionContext} from "@/pages/design-panel/presenters/form";
import {WorkflowFormManager} from "@/pages/design-panel/manager/workflow/form";

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
                    ...values,
                    form: {
                        ...prevState.workflow.form,
                        ...values,
                    }
                }
            }
        });
    }

    public removeWorkflowFormField(formCode: string, fieldCode: string) {
        const workflowFormManager = new WorkflowFormManager(this.state.workflow.form);
        const form = workflowFormManager.removeField(formCode, fieldCode);
        this.updateWorkflowForm(form);
    }

    public removeWorkflowSubForm(code: string) {
        const workflowFormManager = new WorkflowFormManager(this.state.workflow.form);
        const form = workflowFormManager.removeSubForm(code);
        this.updateWorkflowForm(form);
    }

    public addWorkflowSubForm(values: any) {
        const workflowFormManager = new WorkflowFormManager(this.state.workflow.form);
        const form = workflowFormManager.addSubForm(values);
        this.updateWorkflowForm(form);
    }

    private updateWorkflowForm(form: any) {
        this.dispatch((prevState: State) => {
            return {
                ...prevState,
                workflow: {
                    ...prevState.workflow,
                    form
                }
            }
        });
    }

    public updateWorkflowFormField(code: string, values: any) {
        const workflowFormManager = new WorkflowFormManager(this.state.workflow.form);
        const form = workflowFormManager.mergeValue(code, values);
        this.updateWorkflowForm(form);
    }

    public async save() {
        const values = this.formActionContext.save();
        this.updateWorkflow(values);
        const latest = {
            ...this.state,
            workflow: {
                ...this.state.workflow,
                ...values
            }
        };
        await this.api.save(latest.workflow);
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

    public loadDesign(id: string) {
        this.api.load(id).then(result => {
            this.updateWorkflow(result);
        });
    }

    public createDesign() {
        this.api.create().then(result => {
            this.updateWorkflow(result);
        });
    }
}
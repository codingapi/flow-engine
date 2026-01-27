import React from "react";
import {DesignPanelProps, State, TabPanelType} from "../types";
import {Presenter} from "../presenters";
import {FormActionContext} from "../presenters/form";


export class DesignPanelContextScope {

    public state: State;
    private readonly presenter: Presenter;
    private readonly props: DesignPanelProps;
    private readonly formActionContext: FormActionContext;

    constructor(state: State, presenter: Presenter, props: DesignPanelProps) {
        this.state = state;
        this.presenter = presenter;
        this.props = props;
        this.formActionContext = new FormActionContext();
    }

    public getFormActionContext(): FormActionContext {
        return this.formActionContext;
    }

    public close() {
        this.props.onClose?.();
    }

    public save() {
        this.saveData();
        this.close();
    }

    private saveData(){
        const values = this.formActionContext.save();
        this.presenter.updateWorkflow(values);
        console.log('save design ...', values);
    }

    public updateViewPanelTab(tabPanel: TabPanelType) {
        // this.saveData();
        this.presenter.updateViewPanelTab(tabPanel);
    }

    public syncState(state: State) {
        this.state = state;
        this.presenter.syncState(state);
    }

    public getPresenter() {
        return this.presenter;
    }
}


export const DesignPanelContext = React.createContext<DesignPanelContextScope | undefined>(undefined);


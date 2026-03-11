import React from "react";
import {DesignPanelProps, State} from "../types";
import {Presenter} from "../presenters";


export class DesignPanelContextScope {
    private readonly presenter: Presenter;
    private readonly props: DesignPanelProps;

    constructor(presenter: Presenter, props: DesignPanelProps) {
        this.presenter = presenter;
        this.props = props;
    }

    public close() {
        this.presenter.initState();
        this.props.onClose?.();
    }

    public async save(versionName?:string) {
        await this.presenter.save(versionName);
    }

    public syncState(state: State) {
        this.presenter.syncState(state);
    }

    public initState() {
        if(this.props.id){
            this.presenter.loadDesign(this.props.id);
        }else{
            this.presenter.createDesign();
        }
    }

    public getPresenter() {
        return this.presenter;
    }
}


export const DesignPanelContext = React.createContext<DesignPanelContextScope | undefined>(undefined);


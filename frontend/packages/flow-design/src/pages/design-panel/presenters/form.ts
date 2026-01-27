export interface IFormAction {
    save(): any;
}


export class FormActionContext {
    private readonly formActions: IFormAction[];

    constructor() {
        this.formActions = [];
    }

    public addAction(submit: IFormAction) {
        this.formActions.push(submit);
    }

    public save() {
        const value = {};
        for (const form of this.formActions) {
            Object.assign(value, form.save());
        }
        return value;
    }
}
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
        let value = {};
        for (const form of this.formActions) {
            const data = form.save();
            value = Object.assign(value, data);
        }
        return value;
    }
}
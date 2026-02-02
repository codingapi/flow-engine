export interface IFormAction {
    save(): any;

    key(): string;
}


export class FormActionContext {

    private readonly formActions: IFormAction[];

    constructor() {
        this.formActions = [];
    }

    public addAction(submit: IFormAction) {
        const keys = this.formActions.map(item => item.key());
        if (keys.includes(submit.key())) {
            return;
        }
        this.formActions.push(submit);
    }

    public removeAction(key: string) {
        const index = this.formActions.findIndex(item => item.key() === key);
        if (index !== -1) {
            this.formActions.splice(index, 1);
        }
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
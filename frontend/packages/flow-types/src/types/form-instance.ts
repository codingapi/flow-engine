export type NamePath = string | number | boolean | (string | number | boolean)[];

export interface FormInstance{
    getFieldValue: (name: NamePath) => any;
    getFieldsValue: () => any;
    getFieldError: (name: NamePath) => string[];
    resetFields: (fields?: NamePath[]) => void;
    setFieldsValue: (values: any) => void;
    setFieldValue: (name: NamePath, value: any) => void;
    submit: () => void;
}


export interface IFormAction {
    save(): any;

    key(): string;

    validate():Promise<any>;
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

    public async validate() {
        let value = {};
        for (const form of this.formActions) {
            const data = await form.validate();
            value = Object.assign(value, data);
        }
        return value;
    }
}
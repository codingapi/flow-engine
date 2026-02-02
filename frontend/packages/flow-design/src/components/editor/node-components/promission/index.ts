import {FlowForm} from "@/pages/design-panel/types";

export class PromissionManager {

    private data: Map<string, any[]>;
    private readonly onChange: (data: Map<string, any[]>) => void;

    public constructor(data: Map<string, any[]>, onChange: (data: Map<string, any[]>) => void) {
        this.onChange = onChange;
        this.data = data;
    }

    public getDatasource(code: string) {
        return this.data.get(code) || [];
    }

    public initFormPromission(form: FlowForm) {
        if (this.data) {
            return;
        }

        const data = new Map<string, any[]>();
        data.set(form.code, form.fields.map(field => {
            return {
                id: field.id,
                name: field.name,
                readable: true,
                editable: true,
                hidden: false,
            }
        }));
        if (form.subForms) {
            for (const subForm of form.subForms || []) {
                data.set(subForm.code, subForm.fields.map(field => {
                    return {
                        id: field.id,
                        name: field.name,
                        readable: true,
                        editable: true,
                        hidden: false,
                    }
                }));
            }
        }

        this.onChange(data);
        this.data = data;
    }


    private changeFieldValue(code: string, id: any, field: string, value: boolean) {
        const keys = Array.from(this.data.keys());
        const newData = new Map<string, any[]>();
        for (const formCode of keys) {
            const list = this.data.get(formCode) || [];
            if (formCode === code) {
                newData.set(formCode,
                    list.map((item: any) => {
                        if (item.id === id) {
                            return {
                                ...item,
                                [field]: value,
                            }
                        }else {
                            return item;
                        }
                    })
                );
            } else {
                newData.set(formCode, list);
            }
        }
        this.onChange(newData);
    }

    public changeHidden(code: string, id: any, value: boolean) {
        this.changeFieldValue(code, id, 'hidden', value);
    }

    public changeReadable(code: string, id: any, value: boolean) {
        this.changeFieldValue(code, id, 'readable', value);
    }

    public changeEditable(code: string, id: any, value: boolean) {
        this.changeFieldValue(code, id, 'editable', value);
    }

}
import {FlowForm} from "@flow-engine/flow-types";

export class PromissionManager {

    private data: any[];
    private readonly onChange: (data: any[]) => void;
    private readonly form: FlowForm;
    private readonly formList:FlowForm[];

    public constructor(form: FlowForm, data: any[], onChange: (data: any[]) => void) {
        this.form = form;
        this.onChange = onChange;
        this.data = data;
        this.formList =[form,...(form.subForms||[])];
    }

    private getFormFields(code:string){
        for (const form of this.formList){
            if (code === form.code){
                return form.fields;
            }
        }
        return [];
    }


    public getDatasource(code: string) {
        return this.convertFieldsToColumns(code)
    }

    private getFieldName(formCode:string,fieldCode:string){
        for (const form of this.formList){
            if(form.code == formCode){
                const fields = form.fields || [];
                for (const field of fields){
                    if(field.code == fieldCode){
                        return field.name;
                    }
                }
            }
        }
        return null;
    }

    private convertFieldsToColumns(code:string){
        const currentColumns = this.data.filter(item => item.formCode === code) || [];

        return  this.getFormFields(code).map(field => {
            return {
                id: field.id,
                fieldName: field.name,
                fieldCode: field.code,
                formCode: code,
                type: 'WRITE',
            }
        }).map(field => {
            const currentColumnType = currentColumns.find(column => column.fieldCode === field.fieldCode);
            if(currentColumnType){
                return {
                    ...field,
                    type:currentColumnType.type
                }
            }else {
                return field;
            }
        });
    }

    public initFormPromission() {
        if (this.data && this.data.length > 0) {
            return;
        }

        const form = this.form;

        let data: any[] = [];

        const mainList = form.fields.map(field => {
            return {
                id: field.id,
                fieldName: field.name,
                fieldCode: field.code,
                formCode: form.code,
                type: 'WRITE',
            }
        });
        data.push(...mainList);

        if (form.subForms) {
            for (const subForm of form.subForms || []) {
                const list = subForm.fields.map(field => {
                    return {
                        id: field.id,
                        formCode: subForm.code,
                        fieldName: field.name,
                        fieldCode: field.code,
                        type: 'WRITE',
                    }
                });
                data.push(...list);
            }
        }

        this.onChange(data);
        this.data = data;
    }


    private changeFieldValue(code: string, fieldCode: string, value: string) {
        let newData: any[] = [];
        for (const item of this.data) {
            if (item['formCode'] === code && item['fieldCode'] === fieldCode) {
                newData.push({
                    ...item,
                    type: value,
                });
            } else {
                newData.push(item);
            }
        }
        this.onChange(newData);
        this.data = newData;
    }

    public changeHidden(formCode: string, fieldCode: string, value: boolean) {
        if (value) {
            this.changeFieldValue(formCode, fieldCode, 'HIDDEN');
        }
    }

    public changeReadable(formCode: string, fieldCode: string, value: boolean) {
        if (value) {
            this.changeFieldValue(formCode, fieldCode, 'READ');
        }
    }

    public changeEditable(formCode: string, fieldCode: string, value: boolean) {
        if (value) {
            this.changeFieldValue(formCode, fieldCode, 'WRITE');
        }
    }

}
export type NamePath = string | number | boolean | (string | number | boolean)[];

export interface FormInstance{
    getFieldValue: (name: NamePath) => any;
    getFieldsValue: () => any;
    getFieldError: (name: NamePath) => string[];
    resetFields: (fields?: NamePath[]) => void;
    setFieldsValue: (values: any) => void;
    setFiledValue: (name: NamePath, value: any) => void;
    submit: () => void;
}

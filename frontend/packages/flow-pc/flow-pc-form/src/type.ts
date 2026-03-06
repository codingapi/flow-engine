import {FormField} from "@flow-engine/flow-types";

export interface FormItemInputProps {
    defaultValue?: string;
    value?: string;
    onChange?: (value: string) => void;
    placeholder?:string;
    readOnly?:boolean;
}


export interface FormItemProps extends FormField{
    readOnly?:boolean;
}
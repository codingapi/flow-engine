import {FormInstance} from "./form-instance";

export type ViewComponentProps  = {
    form:FormInstance;
    onValuesChange?:(values:any)=>void;
}

import {FormInstance} from "./form-instance";
import {FlowFromMeta} from "@/types/flow-approval";

export type ViewComponentProps  = {
    form:FormInstance;
    onValuesChange?:(values:any)=>void;
    meta:FlowFromMeta;
}

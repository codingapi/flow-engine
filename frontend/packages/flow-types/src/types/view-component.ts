import {FormInstance} from "antd";

export type ViewComponentProps  = {
    form:FormInstance;
    onValuesChange?:(values:any)=>void;
}

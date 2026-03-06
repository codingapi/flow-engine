import {FormInstance} from "./form-instance";
import {FlowForm} from "@/types/flow-approval";


export interface FormViewProps {
    /** 表单操控对象 */
    form: FormInstance;
    /** 表单数据更新事件 */
    onValuesChange?: (values: any) => void;
    /** 表单元数据对象 */
    meta: FlowForm;
    /** 是否预览模式 */
    review:boolean;
}

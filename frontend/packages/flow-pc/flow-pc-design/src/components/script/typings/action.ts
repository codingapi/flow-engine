import {FormInstance} from "antd";
import {FlowActionManager} from "@/components/design-editor/node-components/action/manager";

export interface ActionModalProps {
    nodeId:string;
    open: boolean;
    onCancel: () => void;
    form: FormInstance<any>;
    onFinish: (values: any) => void;
    manager: FlowActionManager;
}

export interface ActionFormProps {
    nodeId:string;
    manager: FlowActionManager;
    form: FormInstance<any>;
    onFinish: (values: any) => void;
}

export interface ActionSelectOption {
    label: string;
    value: string;
    danger?:boolean;
}
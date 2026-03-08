import {FormInstance} from "antd";
import {FlowActionManager} from "@/components/design-editor/node-components/action/manager";

export interface ActionModalProps {
    open: boolean;
    onCancel: () => void;
    form: FormInstance<any>;
    onFinish: (values: any) => void;
    manager: FlowActionManager;
}

export interface ActionFormProps {
    manager: FlowActionManager;
    form: FormInstance<any>;
    onFinish: (values: any) => void;
}

export interface ActionSelectOption {
    label: string;
    value: string;
}
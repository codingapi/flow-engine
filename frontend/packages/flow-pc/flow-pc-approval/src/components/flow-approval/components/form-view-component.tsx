import React from "react";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";
import {ViewBindPlugin} from "@flow-engine/flow-types";
import { Form } from "antd";

interface FormViewComponentProps{
    onValuesChange?:(values:any)=>void;
}

export const FormViewComponent: React.FC<FormViewComponentProps> = (props) => {
    const {state, context} = useApprovalContext();
    const ViewComponent = ViewBindPlugin.getInstance().get(state.flow?.view || 'default');

    const formMeta = state.flow?.form;

    // 是否可合并审批
    const mergeable = state.flow?.mergeable || false;
    const todos = state.flow?.todos || [];
    const viewForms = todos.length>0?todos.map(item => {
        return {
            instance: Form.useForm()[0],
            data: item.data,
        }
    }):[
        {
            instance: Form.useForm()[0],
            data: undefined,
        }
    ]

    React.useEffect(() => {
        viewForms.forEach(item => {
            const formInstance = item.instance;
            const data = item.data;
            context.getPresenter().getFormActionContext().addAction({
                save(): any {
                    return formInstance.getFieldsValue();
                },
                key(): string {
                    return 'view-form'
                }
            });
            formInstance.setFieldsValue(data);
        });
    }, []);

    if (ViewComponent && formMeta) {
        if (mergeable) {
            return (
                <div>
                    <h3>合并审批</h3>
                </div>
            )
        }
        return (
            <>
                {viewForms.map((item, index) => (
                    <ViewComponent
                        key={index}
                        meta={formMeta}
                        form={item.instance as any}
                        onValuesChange={props.onValuesChange}
                    />
                ))}
            </>
        )
    }
}

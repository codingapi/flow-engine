import React from "react";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";
import {ViewBindPlugin} from "@flow-engine/flow-core";
import {Form as AntdForm} from "antd";
import {FlowFormView} from "@flow-engine/flow-pc-form";

interface FormViewComponentProps {
    onValuesChange?: (values: any) => void;
}

export const FormViewComponent: React.FC<FormViewComponentProps> = (props) => {
    const {state, context} = useApprovalContext();
    const review = state.review || false;
    const ViewComponent = ViewBindPlugin.getInstance().get(state.flow?.view || 'default') || FlowFormView;

    const flowForm = state.flow?.form;
    const fieldPermissions = state.flow?.fieldPermissions || [];

    // 是否可合并审批
    const mergeable = state.flow?.mergeable || false;
    const todos = state.flow?.todos || [];
    const viewForms= todos.length > 0 ? todos.map(item => {
        return {
            instance: AntdForm.useForm()[0],
            data: item.data as any,
        }
    }) : [
        {
            instance: AntdForm.useForm()[0],
            data: undefined,
        }
    ]

    React.useEffect(() => {
        viewForms.forEach(item => {
            const formInstance = item.instance;
            const data = item.data;
            context.getPresenter().getFormActionContext().addAction({
                save: () => {
                    return formInstance.getFieldsValue();
                },
                key: () => {
                    return 'view-form'
                },
                validate: () => {
                    return new Promise((resolve,reject) => {
                        formInstance.validateFields()
                            .then(resolve)
                            .catch(reject)
                    })
                }
            });
            formInstance.setFieldsValue(data);
        });
    }, []);

    if (ViewComponent && flowForm) {
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
                        fieldPermissions={fieldPermissions}
                        review={review}
                        meta={flowForm}
                        form={item.instance}
                        onValuesChange={props.onValuesChange}
                    />
                ))}
            </>
        )
    }
}
